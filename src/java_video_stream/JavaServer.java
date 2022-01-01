package java_video_stream;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import javax.imageio.ImageIO;

import javax.swing.*;

import com.sun.jna.NativeLibrary;

//import org.bytedeco.javacv.FFmpegFrameGrabber;

import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.ComponentVideoSurface;
//import uk.co.caprica.vlcj.runtime.windows.WindowsRuntimeUtil;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

public class JavaServer {

	/**
	 * @param args
	 *            the command line arguments
	 */
	public static InetAddress[] inet;
	public static int[] port;
	public static int i;
	static int count = 0;
	public static BufferedReader[] inFromClient;
	public static DataOutputStream[] outToClient;


	public static void main(String[] args) throws Exception
	{
		JavaServer jv = new JavaServer();
	}

	
	
	public JavaServer() throws Exception {

//		Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), LibVlc.class);
//
		NativeLibrary.addSearchPath("libvlc", "C:\\Program Files\\VideoLAN\\VLC");
		NativeLibrary.addSearchPath("libvlccore", "C:\\Program Files\\VideoLAN\\VLC");

		JavaServer.inet = new InetAddress[30];
		port = new int[30];


		ServerSocket welcomeSocket = new ServerSocket(6782);
		System.out.println(welcomeSocket.isClosed());
		Socket connectionSocket[] = new Socket[30];
		inFromClient = new BufferedReader[30];
		outToClient = new DataOutputStream[30];

		DatagramSocket serv = new DatagramSocket(4321);

		byte[] buf = new byte[62000];
		// Socket[] sc = new Socket[5];
		DatagramPacket dp = new DatagramPacket(buf, buf.length);

		VideoShow canv = new VideoShow();
		System.out.println("Gotcha");

		// OutputStream[] os = new OutputStream[5];

		i = 0;
		
		SThread[] st = new SThread[30];
		

		while (true) {

			System.out.println(serv.getPort());
			serv.receive(dp);
			System.out.println(new String(dp.getData()));
			buf = "starts".getBytes();

			inet[i] = dp.getAddress();
			port[i] = dp.getPort();

			DatagramPacket dsend = new DatagramPacket(buf, buf.length, inet[i], port[i]);
			serv.send(dsend);

			Vidthread sendvid = new Vidthread(serv);

			System.out.println("waiting\n ");
			connectionSocket[i] = welcomeSocket.accept();
			System.out.println("connected " + i);

			inFromClient[i] = new BufferedReader(new InputStreamReader(connectionSocket[i].getInputStream()));
			outToClient[i] = new DataOutputStream(connectionSocket[i].getOutputStream());
			outToClient[i].writeBytes("Connected: from Server\n");

			
			st[i] = new SThread(i);
			st[i].start();
			
			if(count == 0)
			{
				Sentencefromserver sen = new Sentencefromserver();
				sen.start();
				count++;
			}

			System.out.println(inet[i]);
			sendvid.start();

			i++;

			if (i == 30) {
				break;
			}
		}
	}
}

class Vidthread extends Thread {

	int clientno;
	// InetAddress iadd = InetAddress.getLocalHost();
	JFrame jf = new JFrame("scrnshots before sending");
	JLabel jleb = new JLabel();

	DatagramSocket soc;

	Robot rb = new Robot();

	byte[] outbuff = new byte[62000];

	BufferedImage mybuf;
	ImageIcon img;
	Rectangle rc;
	
	int bord = VideoShow.panel.getY() - VideoShow.frame.getY();

	// Rectangle rv = new Rectangle(d);
	public Vidthread(DatagramSocket ds) throws Exception {
		soc = ds;

		System.out.println(soc.getPort());
		jf.setSize(500, 400);
		jf.setLocation(500, 400);
		jf.setVisible(true);
	}

	public void run() {
		while (true) {
			try {

				int num = JavaServer.i;

				rc = new Rectangle(new Point(VideoShow.frame.getX() + 8, VideoShow.frame.getY() + 27),
						new Dimension(VideoShow.panel.getWidth(), VideoShow.frame.getHeight() / 2));

				// System.out.println("another frame sent ");

				mybuf = rb.createScreenCapture(rc);

				img = new ImageIcon(mybuf);

				jleb.setIcon(img);
				jf.add(jleb);
				jf.repaint();
				jf.revalidate();
				// jf.setVisible(true);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				
				ImageIO.write(mybuf, "jpg", baos);
				
				outbuff = baos.toByteArray();

				for (int j = 0; j < num; j++) {
					DatagramPacket dp = new DatagramPacket(outbuff, outbuff.length, JavaServer.inet[j],
							JavaServer.port[j]);

					soc.send(dp);
					baos.flush();
				}
				Thread.sleep(15);
			} catch (Exception e) {

			}
		}

	}

}

//Thực hiện luồng video show
class VideoShow {


	private MediaPlayerFactory mediaPlayerFactory;


	private EmbeddedMediaPlayer  mediaPlayer;


	public static JPanel panel;
	public static JPanel myjp;
	private Canvas canvas;
	public static JFrame frame;
	public static JTextArea ta;
	public static JTextArea txinp;
	public static int xpos = 0, ypos = 0;
	String url = "D:\\DownLoads\\Video\\freerun.MP4";

	public VideoShow() {


		panel = new JPanel();
		panel.setLayout(new BorderLayout());

		JPanel mypanel = new JPanel();
		mypanel.setLayout(new GridLayout(2, 1));


		canvas = new Canvas();
		canvas.setBackground(Color.BLACK);


		panel.add(canvas, BorderLayout.CENTER);
		mediaPlayerFactory = new MediaPlayerFactory();
		mediaPlayer = mediaPlayerFactory.mediaPlayers().newEmbeddedMediaPlayer();
		ComponentVideoSurface videoSurface = mediaPlayerFactory.videoSurfaces().newVideoSurface(canvas);
		mediaPlayer.videoSurface().set(videoSurface);



		frame = new JFrame("Server show");
		// frame.setLayout(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLocation(200, 0);
		frame.setSize(640, 960);
		frame.setAlwaysOnTop(true);

		mypanel.add(panel);
		frame.add(mypanel);
		frame.setVisible(true);
		xpos = frame.getX();
		ypos = frame.getY();

		// Playing the video

		myjp = new JPanel(new GridLayout(5, 1));
		JPanel pControls = new JPanel(new GridLayout(1,3));
		Button btnPre = new Button("Previous");
		Button btnPause = new Button("Pause");
		Button btnSkip = new Button("Skip");

		pControls.add(btnPre);
		pControls.add(btnPause);
		pControls.add(btnSkip);
		Button bn = new Button("Choose File");

		myjp.add(pControls);
		myjp.add(bn);
		Button sender = new Button("Send Text");

		JScrollPane jpane = new JScrollPane();
		jpane.setSize(300, 200);
		// ta.setEditable(false);
		ta = new JTextArea();
		txinp = new JTextArea();
		jpane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		jpane.add(ta);
		jpane.setViewportView(ta);
		myjp.add(jpane);
		myjp.add(txinp);
		myjp.add(sender);
		ta.setText("Initialized");

		ta.setCaretPosition(ta.getDocument().getLength());

		mypanel.add(myjp);
		mypanel.revalidate();
		mypanel.repaint();

		bn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				JFileChooser jf = new JFileChooser();
				jf.showOpenDialog(frame);
				File f;
				f = jf.getSelectedFile();
				url = f.getPath();
				System.out.println(url);
				ta.setText("check text\n");
				ta.append(url+"\n");
				mediaPlayer.media().play(url);
			}
		});

		btnPause.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				mediaPlayer.controls().pause();
			}
		});

		btnPre.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				mediaPlayer.controls().skipTime(-3000);
			}
		});

		btnSkip.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				mediaPlayer.controls().skipTime(3000);
			}
		});
		sender.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				Sentencefromserver.sendingSentence = txinp.getText();
				txinp.setText(null);
				VideoShow.ta.append("From Myself: " + Sentencefromserver.sendingSentence + "\n");
				VideoShow.myjp.revalidate();
				VideoShow.myjp.repaint();
			}
		});

	}
}

class SThread extends Thread {

	public static String clientSentence;
	int srcid;
	BufferedReader inFromClient = JavaServer.inFromClient[srcid];
	DataOutputStream outToClient[] = JavaServer.outToClient;

	public SThread(int a) {
		srcid = a;
	}

	public void run() {
		while (true) {
			try {

				clientSentence = inFromClient.readLine();
				// clientSentence = inFromClient.readLine();

				System.out.println("From Client " + srcid + ": " + clientSentence);
				VideoShow.ta.append("From Client " + srcid + ": " + clientSentence + "\n");
				
				for(int i=0; i<JavaServer.i; i++){
                    
                    if(i!=srcid)
                        outToClient[i].writeBytes("Client "+srcid+": "+clientSentence + '\n');	//'\n' is necessary
                }
				
				VideoShow.myjp.revalidate();
				VideoShow.myjp.repaint();

					} catch (Exception e) {
			}

		}
	}
}

class Sentencefromserver extends Thread {
	
	public static String sendingSentence;
	
	public Sentencefromserver() {

	}

	public void run() {

		while (true) {

			try {

				if(sendingSentence.length()>0)
				{
					for (int i = 0; i < JavaServer.i; i++) {
						JavaServer.outToClient[i].writeBytes("From Server: "+sendingSentence+'\n');
						
					}
					sendingSentence = null;
				}

			} catch (Exception e) {

			}
		}
	}
}
