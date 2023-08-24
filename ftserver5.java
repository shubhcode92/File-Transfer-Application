import java.util.*;
import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
class RequestProcessor extends Thread
{
private Socket socket;
private FTServerFrame fsf1;
String id;
RequestProcessor(Socket socket,String id,FTServerFrame fsf1)
{
this.id = id;
this.fsf1 = fsf1;
this.socket = socket;
start();
}
public void run()
{
try
{
// receiving header from client
SwingUtilities.invokeLater(new Thread(){
public void run()
{
fsf1.updateLog("Client connected with id : " + id);
}
});
InputStream is = socket.getInputStream();
byte tmp[] = new byte[1024];
byte header[] = new byte[1024];
int i=0;
int j=0;
int bytesToReceive = 1024;
int byteReadCount = 0;
while(j<bytesToReceive)
{
byteReadCount = is.read(tmp);
if(byteReadCount == -1) continue;
for(int k=0;k<byteReadCount;k++)
{
header[i] = tmp[k];
i++;
}
j = j + byteReadCount;
}

//sending confirmation to server of receiving header
byte ack[] = new byte[1];
ack[0] = 1;
OutputStream os = socket.getOutputStream();
os.write(ack,0,1);
os.flush();

// parsing header
int requestLength = 0;
i = 0;
j = 1;
while(header[i]!=',')
{
requestLength = requestLength + (header[i]*j);
i++;
j *= 10;
}
i++;
StringBuffer sb = new StringBuffer();
while(i<=1023)
{
sb.append((char)header[i]);
i++;
}
String fileName = sb.toString().trim();
int rl = requestLength;
String fn = fileName;
SwingUtilities.invokeLater(()->{
fsf1.updateLog("Receiving file : " + fn + " of lenght : " + rl);
});

// if file already exists delete it 
String filePath = "C:/Users/Microgmg/Documents/java_tut/Networking/example5/uploads/" + fileName; // edit here
File file = new File(filePath);
if(file.exists() == true) file.delete();

// receiving data from cilent 
FileOutputStream receiveFileChunk = new FileOutputStream(filePath,true);
int bytesReadCount = 0;
bytesToReceive = requestLength;
final int bytesToRead = bytesToReceive/1024; // Edit later if TM said
j=0;
long m=0;
while(m<bytesToReceive)
{
byteReadCount = is.read(tmp);
if(byteReadCount == -1) continue;
receiveFileChunk.write(tmp);  // come
receiveFileChunk.flush();
m = m  + byteReadCount;
}
receiveFileChunk.close();

// sending confirmation to server of getting file
ack[0] = 1;
os.write(ack,0,1);
os.flush();
socket.close();
SwingUtilities.invokeLater(()->{
fsf1.updateLog("File saved to + " + file.getAbsolutePath());
fsf1.updateLog("COnnection with client whose id is : " + this.id + " closed");
});
}catch(Exception ie)
{
System.out.println(ie);
}
}//run
}//class rp

class FTServer extends Thread
{
private ServerSocket serverSocket;
private FTServerFrame fsf;
private Socket socket;
FTServer(FTServerFrame fsf)
{
this.fsf = fsf;
}

public void run()
{
try
{
serverSocket = new ServerSocket(5500);
startListening();
}catch(Exception e)
{
System.out.println(e);
}
}//run
public void shutDown()
{
try
{
serverSocket.close();
}catch(Exception e)
{
System.out.println(e); // remove after test
}
}
public void startListening()
{
try
{
RequestProcessor requestProcessor;
while(true)
{
SwingUtilities.invokeLater(new Thread(){
public void run()
{
fsf.updateLog("server is ready to listen on port 5500");
}
});
socket = serverSocket.accept();
requestProcessor = new RequestProcessor(socket,UUID.randomUUID().toString(),this.fsf);
}//while
}catch(Exception e)
{
System.out.println("Server stop listening");
System.out.println(e);
}
}// end method

}//ends FTServer

class FTServerFrame extends JFrame implements ActionListener
{
private JButton button;
private JTextArea jta;
private Container container;
private FTServer server;
private JScrollPane jsp;
private Boolean serverState = false;
FTServerFrame()
{
container = getContentPane();
container.setLayout(new BorderLayout());
button = new JButton("Start");
jta = new JTextArea();
jsp = new JScrollPane(jta,ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
container.add(jsp,BorderLayout.CENTER);
container.add(button,BorderLayout.SOUTH);
setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
setLocation(100,400);
setVisible(true);
setSize(900,250);
button.addActionListener(this);
}

public void updateLog(String msg)
{
this.jta.append(msg + "\n");
}

public void actionPerformed(ActionEvent ev)
{
if(serverState == false)
{
server = new FTServer(this);
server.start();
button.setText("Stop");
serverState = true;
}
else
{
jta.append("Server stopped\n");
server.shutDown(); 
button.setText("Start");
serverState = false;
}
}

public static void main(String[] aa)
{
FTServerFrame server1 = new FTServerFrame();
}
}
