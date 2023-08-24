
import java.io.*;
import java.net.*;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.table.*;
import java.util.*;

class FileUploadEvent
{
private File file;
private String uploaderId;
private long numberOfBytesUploaded;
public FileUploadEvent(File file,String uploaderId,long numberOfBytesUploaded)
{
this.file = file;
this.uploaderId = uploaderId;
this.numberOfBytesUploaded = numberOfBytesUploaded;
}
public File getFile()
{
return this.file;
}
public String getUploaderId()
{
return this.uploaderId;
}
public long getNumberOfBytesUploaded()
{
return this.numberOfBytesUploaded;
}
}// FileUploadEvent ends

interface FileUploadListener
{
public void fileUploadStatusChanged(FileUploadEvent fileUploadEvent);
}

class FileModel extends AbstractTableModel
{
private ArrayList<File> files;
FileModel()
{
this.files = new ArrayList<>();
}
public ArrayList<File> getFiles()
{
return files;
}

public int getRowCount()
{
return this.files.size();
}
public int getColumnCount()
{
return 2;
}
public String getColumnName(int c)
{
if(c==0) return "S.no.";
return "Files";
}
public Class getColumnClass(int c)
{
if(c==0) return Integer.class;
return String.class;
}
public boolean isCellEditable(int r,int c)
{
return false;
}
public Object getValueAt(int r,int c)
{
if(c==0) return (r+1);
return this.files.get(r).getAbsolutePath();
}
public void add(File file)
{
this.files.add(file);
fireTableDataChanged();
}
}


class FTClientFrame extends JFrame
{
private String host;
private int portNumber;
private FileSelectionPanel fileSelectionPanel;
private FileUploadViewPanel fileUploadViewPanel;
private Container container;

FTClientFrame(String host,int portNumber)
{
this.host = host;
this.portNumber = portNumber;
fileSelectionPanel = new FileSelectionPanel();
fileUploadViewPanel = new FileUploadViewPanel();
container = getContentPane();
container.setLayout(new GridLayout(1,2));
container.add(fileSelectionPanel);
container.add(fileUploadViewPanel);
setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
setSize(800,400);
setLocation(100,100);
setVisible(true);
}

// left section
class FileSelectionPanel extends JPanel implements ActionListener
{
private JLabel titleLabel;
private JTable table;
private FileModel model;
private JScrollPane jsp;
private JButton button;
FileSelectionPanel()
{
setLayout(new BorderLayout());
titleLabel = new JLabel("Selected File(s)");
model = new FileModel();
table = new JTable(model);
jsp = new JScrollPane(table,ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
button = new JButton("+");
button.addActionListener(this);
add(titleLabel,BorderLayout.NORTH);
add(jsp,BorderLayout.CENTER);
add(button,BorderLayout.SOUTH);
}
public ArrayList<File> getFiles()
{
return model.getFiles();
}

public void actionPerformed(ActionEvent ev)
{
JFileChooser jfc = new JFileChooser();
jfc.setCurrentDirectory(new File("."));
int selectedOption = jfc.showOpenDialog(this);
if(selectedOption == jfc.APPROVE_OPTION);
{
File selectedFile = jfc.getSelectedFile();
model.add(selectedFile);
}
}
} // inner class ends

// right section
class FileUploadViewPanel extends JPanel implements ActionListener,FileUploadListener
{
private JButton uploadFilesButton;
private JPanel progressPanelsContainer;
private JScrollPane jsp;
private ArrayList<ProgressPanel> progressPanels;
private ArrayList<File> files;
private ArrayList<FileUploadThread> fileUploaders;
private boolean uploadButtonStatus = true;
FileUploadViewPanel()
{
uploadFilesButton = new JButton("Upload File(s)");
setLayout(new BorderLayout());
add(uploadFilesButton,BorderLayout.NORTH);
uploadFilesButton.addActionListener(this); 
}


public void actionPerformed(ActionEvent ev)
{

files = fileSelectionPanel.getFiles();

if(files.size()==0)
{
JOptionPane.showMessageDialog(FTClientFrame.this,"No Files selected to upload");
return;
}
ProgressPanel progressPanel;
progressPanels = new ArrayList<>();
progressPanelsContainer = new JPanel();
progressPanelsContainer.setLayout(new GridLayout(files.size(),1));
fileUploaders = new ArrayList<>();
FileUploadThread fut;
String uploaderId;
for(File file:files)
{
uploaderId = UUID.randomUUID().toString();
progressPanel = new ProgressPanel(uploaderId,file);
progressPanels.add(progressPanel);
progressPanelsContainer.add(progressPanel);
fut = new FileUploadThread(this,uploaderId,file,host,portNumber);
fileUploaders.add(fut);
}
jsp = new JScrollPane(progressPanelsContainer,ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
add(jsp,BorderLayout.CENTER);
this.revalidate(); // to refresh the panel
for(FileUploadThread fileUploadThread:fileUploaders)
{
fileUploadThread.start();
}
}// actionP

public void fileUploadStatusChanged(FileUploadEvent fileUploadEvent)
{
String uploaderId = fileUploadEvent.getUploaderId();
long numberOfBytesUploaded = fileUploadEvent.getNumberOfBytesUploaded();
File file = fileUploadEvent.getFile();
for(ProgressPanel progressPanel:progressPanels)
{
if(progressPanel.getId().equals(uploaderId))
{
progressPanel.updateProgressBar(numberOfBytesUploaded);
break;
}
}
}

class ProgressPanel extends JPanel
{
private File file;
private JLabel fileNameLabel;
private JProgressBar progressBar;
private long fileLength;
private String id;
public ProgressPanel(String id,File file)
{
this.id = id;
this.file = file;
this.fileLength = file.length();
fileNameLabel = new JLabel("Uploading : " + file.getAbsolutePath());
progressBar = new JProgressBar(1,100);
setLayout(new GridLayout(2,1));
add(fileNameLabel);
add(progressBar);
}
public String getId()
{
return this.id;
}
public void updateProgressBar(long bytesUploaded)
{
int percentage;
if(bytesUploaded == fileLength)
{
percentage = 100;
fileNameLabel.setText("Uploaded : " + file.getAbsolutePath());
}
else
{
percentage = (int)((bytesUploaded*100)/fileLength);
}
progressBar.setValue(percentage);
} 
}// progressbar ends
}// right section


public static void main(String[] aa)
{
FTClientFrame fcf = new FTClientFrame("localhost",5500);
}
}// FTClientFrame

class FileUploadThread extends Thread
{
private FileUploadListener fileUploadListener;
private String id;
private File file;
private String host;
private int portNumber;
FileUploadThread(FileUploadListener fileUploadListener,String id,File file,String host,int portNumber)
{
this.fileUploadListener=fileUploadListener;
this.id = id;
this.file = file;
this.host = host;
this.portNumber = portNumber;
}

public void run()
{
try
{
// lenght of file
long lengthOfFile = (file.length());
String fileToSend = file.getName();
// header for server (name and size)
int requestLength  = (int)lengthOfFile;
byte header[] = new byte[1024];
int i = 0;
while(requestLength>0)
{
header[i] = (byte)(requestLength%10);
requestLength  = requestLength  /10;
i++;
}
header[i] = (byte)',';
i++;
int x = fileToSend.length();
int k=0;
while(k<x)
{
header[i] = (byte)fileToSend.charAt(k);
i++;
k++;
}
while(i<=1023)
{
header[i] = (byte)32;
i++;
}
Socket socket =  new Socket(host,portNumber); // error line
OutputStream os = socket.getOutputStream();
os.write(header,0,1024); // kya bhejna h, kha se bhejna h, kitna bhejna h
os.flush();

//to get confirmation of sending header to server
InputStream is = socket.getInputStream();
byte ack[] = new byte[1]; // to get confirmation of geting header from server
int bytesReadCount;
while(true)
{
bytesReadCount = is.read(ack); 
if(bytesReadCount == -1) continue;
break;
}
// sending file to server
FileInputStream sendFileChunk = new FileInputStream(file);
int chunkSize = 1024;
byte b[] = new byte[chunkSize];
int j=0;
int bytesReadCountFromFile=0;
while(true)
{
j = sendFileChunk.read(b);
if(j==-1) break;
os.write(b,0,j);
os.flush();
bytesReadCountFromFile = bytesReadCountFromFile + j;
int brc = bytesReadCountFromFile;
SwingUtilities.invokeLater(()->{
FileUploadEvent fue = new FileUploadEvent(this.file,this.id,brc);
fileUploadListener.fileUploadStatusChanged(fue);
});
}
sendFileChunk.close();

// getting confirmation for receiving receiving file from server
while(true)
{
bytesReadCount = is.read(ack); 
if(bytesReadCount == -1) continue;
break;
}
socket.close();
}catch(Exception e)
{
System.out.println(e);
}//catch
}//main
}//class