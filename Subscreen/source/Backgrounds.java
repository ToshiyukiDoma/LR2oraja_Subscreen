package lr2touch;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.*;
import javax.imageio.stream.*;
import javax.swing.*;

/** Local image gallery and bounded optional FFmpeg video playback. */
public final class Backgrounds implements AutoCloseable {

 final Touch ui;volatile boolean paused,closed;Path video;volatile Decoder decoder;volatile int framesDecoded;
 Backgrounds(Touch ui){this.ui=ui;}
 void play(Path file){stop();if(closed)return;if(findFFmpeg(Touch.base,System.getenv("PATH"))==null){ui.message("VIDEO NEEDS FFMPEG.EXE IN SUBSCREEN / TOOLS OR WINDOWS PATH · SEE README");return;}video=file;if(!paused)startDecoder();}
 void startDecoder(){if(video==null||closed||paused)return;decoder=new Decoder(video);decoder.start();}
 void pause(boolean value){if(paused==value)return;paused=value;if(value){stopDecoder();}else startDecoder();}
 void stopDecoder(){Decoder d=decoder;decoder=null;if(d!=null)d.close();}
 void stop(){stopDecoder();video=null;ui.surface.videoActive=false;ui.surface.videoFrame=null;ui.surface.repaint();}
 public void close(){closed=true;stop();}
 final class Decoder implements AutoCloseable {
  static final int WIDTH=1280,HEIGHT=720,BYTES=WIDTH*HEIGHT*3;
  final Path source;final ArrayBlockingQueue<byte[]> pool=new ArrayBlockingQueue<>(3);final java.util.concurrent.atomic.AtomicReference<byte[]> latest=new java.util.concurrent.atomic.AtomicReference<>();final java.util.concurrent.atomic.AtomicBoolean repaintQueued=new java.util.concurrent.atomic.AtomicBoolean();volatile boolean stopped;volatile Process process;BufferedImage frame;
  Decoder(Path source){this.source=source;}
  void start(){Thread t=new Thread(this::run,"LR2Touch-video");t.setDaemon(true);t.start();}
  void run(){try{
   for(int i=0;i<3;i++)pool.add(new byte[BYTES]);if(stopped)return;
   process=new ProcessBuilder(findFFmpeg(Touch.base,System.getenv("PATH")).toString(),"-v","error","-nostdin","-threads","1","-filter_threads","1","-re","-stream_loop","-1","-i",source.toString(),"-map","0:v:0","-an","-vf","scale=1280:720,fps=30","-pix_fmt","bgr24","-threads","1","-f","rawvideo","pipe:1").redirectError(Touch.base.resolve("video-playback.log").toFile()).start();
   try(InputStream in=process.getInputStream()){while(!stopped){byte[] data=pool.poll(1,TimeUnit.SECONDS);if(data==null)continue;int offset=0,n;while(offset<data.length&&(n=in.read(data,offset,data.length-offset))>=0)offset+=n;if(offset<data.length)break;byte[] old=latest.getAndSet(data);if(old!=null)pool.offer(old);queueFrame();}}
   if(!stopped)ui.message("Video playback stopped · see video-playback.log");
  }catch(Exception e){if(!stopped)ui.message("Could not play video · "+e.getClass().getSimpleName());}finally{if(process!=null)process.destroy();}}
  void queueFrame(){if(repaintQueued.compareAndSet(false,true))SwingUtilities.invokeLater(()->{
   byte[] data=latest.getAndSet(null);try{if(data!=null&&!stopped&&decoder==this){if(frame==null)frame=new BufferedImage(WIDTH,HEIGHT,BufferedImage.TYPE_3BYTE_BGR);System.arraycopy(data,0,((java.awt.image.DataBufferByte)frame.getRaster().getDataBuffer()).getData(),0,data.length);ui.surface.videoFrame=frame;ui.surface.videoActive=true;framesDecoded++;ui.surface.repaint();}}finally{if(data!=null)pool.offer(data);repaintQueued.set(false);if(latest.get()!=null&&!stopped)queueFrame();}
  });}
  public void close(){stopped=true;if(process!=null)process.destroy();byte[] pending=latest.getAndSet(null);if(pending!=null)pool.offer(pending);}
 }
 static Path findFFmpeg(Path base,String path){
  Path local=base.resolve("tools/ffmpeg.exe");if(Files.isRegularFile(local))return local.toAbsolutePath();
  if(path!=null)for(String entry:path.split(java.util.regex.Pattern.quote(File.pathSeparator))){String folder=entry.trim();if(folder.startsWith("\"")&&folder.endsWith("\""))folder=folder.substring(1,folder.length()-1);if(folder.isBlank())continue;try{Path executable=Paths.get(folder).resolve("ffmpeg.exe");if(Files.isRegularFile(executable))return executable.toAbsolutePath();}catch(InvalidPathException ignored){}}
  return null;
 }
 static boolean image(Path p){return p.getFileName().toString().toLowerCase(Locale.ROOT).matches(".*\\.(png|jpg|jpeg|bmp|gif)$");}
 static boolean video(Path p){return p.getFileName().toString().toLowerCase(Locale.ROOT).matches(".*\\.(mp4|mkv|webm|avi|mov|m4v)$");}
 void gallery(){
  if(ui.concentrating){ui.message("Choose backgrounds after returning to song selection");return;}
  Path root=Touch.base.resolve("backgrounds");try{Files.createDirectories(root.resolve("import"));}catch(IOException e){ui.message("Cannot open backgrounds directory");return;}
  JDialog d=new JDialog(ui.frame,"BACKGROUND GALLERY",true);d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);JPanel body=new JPanel(new BorderLayout(14,16));body.setBackground(ui.card);body.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
  JPanel header=Touch.panel(new GridLayout(0,1,0,8));header.add(Touch.label("Background gallery",32,Font.BOLD));header.add(Touch.label("Images: Subscreen / backgrounds",24,0));header.add(Touch.label("Videos: Subscreen / backgrounds / import",24,0));body.add(header,BorderLayout.NORTH);
  JPanel grid=Touch.panel(new GridLayout(2,3,12,12));body.add(grid);JPanel footer=Touch.panel(new GridLayout(1,4,10,0));int[] page={0};
  Runnable[] refresh={null};refresh[0]=()->{try{java.util.List<Path> files=new ArrayList<>();for(Path folder:new Path[]{root,root.resolve("import"),root.resolve("optimized")})if(Files.isDirectory(folder))try(var stream=Files.list(folder)){stream.filter(Files::isRegularFile).filter(p->(image(p)||video(p))&&!p.getFileName().toString().endsWith(".mp4.jpg")).sorted().forEach(files::add);}int max=Math.max(0,(files.size()-1)/6);page[0]=Math.min(page[0],max);grid.removeAll();for(int i=page[0]*6;i<Math.min(files.size(),page[0]*6+6);i++){Path file=files.get(i);
    JButton b=ui.button(file.getFileName().toString(),()->{stop();if(video(file)){play(file);ui.settings.setProperty("video",root.relativize(file).toString());ui.settings.remove("background");}else{ui.loadBackground(file);ui.settings.setProperty("background",root.relativize(file).toString());ui.settings.remove("video");}ui.save();d.dispose();ui.returnFocus();});
    b.setVerticalTextPosition(SwingConstants.BOTTOM);b.setHorizontalTextPosition(SwingConstants.CENTER);b.setFont(new Font("Yu Gothic UI",0,22));
    Path thumb=image(file)?file:file.resolveSibling(file.getFileName()+".jpg");try{BufferedImage image=thumbnail(thumb);if(image!=null)b.setIcon(new ImageIcon(image));}catch(Exception ignored){}grid.add(b);
   }if(files.isEmpty())grid.add(Touch.label("Add files to the folders above, then Refresh",24,0));ui.themeTree(grid);grid.revalidate();grid.repaint();}catch(IOException e){ui.message("Gallery could not read a folder");}};
  footer.add(ui.button("Previous",()->{page[0]=Math.max(0,page[0]-1);refresh[0].run();}));footer.add(ui.button("Next",()->{page[0]++;refresh[0].run();}));footer.add(ui.button("Refresh",refresh[0]));footer.add(ui.button("Done",d::dispose));body.add(footer,BorderLayout.SOUTH);refresh[0].run();ui.themeTree(body);d.setContentPane(body);d.setSize(Math.min(1240,ui.frame.getWidth()-20),Math.min(960,ui.frame.getHeight()-20));d.setLocationRelativeTo(ui.frame);d.setVisible(true);d.dispose();
 }
 static BufferedImage thumbnail(Path p)throws IOException{
  if(!Files.isRegularFile(p))return null;try(ImageInputStream in=ImageIO.createImageInputStream(p.toFile())){Iterator<ImageReader> it=ImageIO.getImageReaders(in);if(!it.hasNext())return null;ImageReader r=it.next();try{r.setInput(in);int w=r.getWidth(0),h=r.getHeight(0);var param=r.getDefaultReadParam();int sub=Math.max(1,(int)Math.ceil(Math.max(w/320.0,h/180.0)));param.setSourceSubsampling(sub,sub,0,0);return r.read(0,param);}finally{r.dispose();}}
 }

}
