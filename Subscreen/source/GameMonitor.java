package lr2touch;
import bms.player.beatoraja.*;
import bms.player.beatoraja.play.BMSPlayer;
import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.select.bar.SongBar;
import bms.player.beatoraja.song.SongData;
import java.awt.*;
import java.util.concurrent.*;
import javax.swing.*;

/** Small read-only snapshots. Never scans tables or queries the song database. */
public final class GameMonitor implements AutoCloseable {
 final Touch ui;final ScheduledExecutorService executor=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"LR2Touch-status");t.setDaemon(true);return t;});
 long lastPlaying;boolean lastConcentration;
 GameMonitor(Touch ui){this.ui=ui;}
 void start(){executor.scheduleWithFixedDelay(this::poll,0,250,TimeUnit.MILLISECONDS);}
 void poll(){
  try{if(!(ui.attachedMain instanceof MainController))return;MainController main=(MainController)ui.attachedMain;MainState state=main.getCurrentState();if(state==null)return;
   if(state instanceof BMSPlayer&&((BMSPlayer)state).getState()==BMSPlayer.STATE_PLAY){long now=System.nanoTime();if(now-lastPlaying<TimeUnit.SECONDS.toNanos(1))return;lastPlaying=now;}
   Snapshot s=read(main,state);SwingUtilities.invokeLater(()->ui.receive(s));
  }catch(Throwable ignored){/* A state may be disposed between reads; retry the next bounded sample. */}
 }
 static Snapshot read(MainController main,MainState state){
  Snapshot s=new Snapshot();s.profileId=main.getPlayerResource().getPlayerConfig().getId();s.profileName=main.getPlayerResource().getPlayerConfig().getName();PlayerResource resource=main.getPlayerResource();s.concentration=state instanceof BMSPlayer||state.getClass().getSimpleName().equals("MusicDecide");
  SongData song=resource.getSongdata();if(state instanceof MusicSelector){Object bar=((MusicSelector)state).getBarManager().getSelected();song=bar instanceof SongBar?((SongBar)bar).getSongData():null;}
  if(song!=null){s.title=song.getTitle();String subtitle=song.getSubtitle();if(subtitle!=null&&!subtitle.isBlank())s.title+=" "+subtitle;s.artist=song.getArtist();s.genre=song.getGenre();int d=song.getDifficulty();String[] names={"Unknown","Beginner","Normal","Hyper","Another","Insane"};s.difficulty=(d>=0&&d<names.length?names[d]:"Difficulty "+d)+" · Lv "+song.getLevel();}
  if(s.concentration){s.table=(java.util.Objects.toString(resource.getTablename(),"")+" "+java.util.Objects.toString(resource.getTablelevel(),"")).trim();s.notesReady=resource.getBMSModel()!=null;s.audio=main.getAudioProcessor().getProgress();s.bga=resource.getBGAManager().getProgress();s.loading=!(state instanceof BMSPlayer)||((BMSPlayer)state).getState()==BMSPlayer.STATE_PRELOAD;if(state instanceof BMSPlayer&&((BMSPlayer)state).getLanerender()!=null)s.currentDuration=((BMSPlayer)state).getLanerender().getCurrentDuration();s.phase=s.loading?"Preparing chart":"Concentration mode";}
  return s;
 }
 public void close(){executor.shutdownNow();}
 static final class Snapshot {String profileId="",profileName="";int currentDuration=-1;boolean concentration,loading,notesReady;float audio,bga;String title="",artist="",genre="",difficulty="",table="",phase="";}
 static final class MarqueeLabel extends JLabel {long start=System.nanoTime();String last="";MarqueeLabel(String text,int size,int style){super(text);setFont(new Font("Yu Gothic UI",style,size));setHorizontalAlignment(SwingConstants.CENTER);putClientProperty("preserveCase",true);}public Dimension getPreferredSize(){return new Dimension(400,getFontMetrics(getFont()).getHeight()+10);}public Dimension getMinimumSize(){return new Dimension(100,getPreferredSize().height);}boolean overflow(){return isVisible()&&getWidth()>0&&getFontMetrics(getFont()).stringWidth(getText())>getWidth()-16;}protected void paintComponent(Graphics graphics){String value=getText();if(value==null||value.isEmpty())return;if(!value.equals(last)){last=value;start=System.nanoTime();}Graphics2D g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);java.awt.font.TextLayout layout=new java.awt.font.TextLayout(value,getFont(),g.getFontRenderContext());double width=layout.getAdvance(),available=getWidth()-16,x=(getWidth()-width)/2;double excess=width-available;if(excess>0){double seconds=(System.nanoTime()-start)/1e9,travel=excess/35.0,cycle=travel+4,phase=seconds%cycle;x=8-Math.min(excess,Math.max(0,phase-2)*35);}double y=(getHeight()-layout.getAscent()-layout.getDescent())/2+layout.getAscent();Shape outline=layout.getOutline(java.awt.geom.AffineTransform.getTranslateInstance(x,y));Color fg=getForeground();g.setColor(fg.getRed()+fg.getGreen()+fg.getBlue()>400?new Color(10,20,36,235):new Color(238,248,255,235));g.setStroke(new BasicStroke(3,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.draw(outline);g.setColor(fg);g.fill(outline);g.dispose();}}
 static final class SongPanel extends JPanel {
  final Touch ui;final MarqueeLabel genre=new MarqueeLabel("",28,0),title=new MarqueeLabel("Choose your next track",58,Font.BOLD),artist=new MarqueeLabel("",30,0),table=new MarqueeLabel("",26,0);final JLabel phase=Touch.label("",26,Font.BOLD),notes=Touch.label("",24,0);
  final JProgressBar audio=new JProgressBar(0,100),bga=new JProgressBar(0,100);final JPanel loading=Touch.panel(new GridLayout(0,1,0,12));String previous="";final javax.swing.Timer marquee=new javax.swing.Timer(40,e->{if(!isShowing()){((javax.swing.Timer)e.getSource()).stop();return;}for(MarqueeLabel label:new MarqueeLabel[]{genre,title,artist,table})if(label.overflow())label.repaint();});
  SongPanel(Touch ui){super(new GridBagLayout());this.ui=ui;setOpaque(false);JPanel text=Touch.panel(new GridLayout(0,1,0,14));for(JLabel l:new JLabel[]{genre,title,artist,table}){l.setHorizontalAlignment(SwingConstants.CENTER);l.putClientProperty("preserveCase",true);text.add(l);}loading.add(phase);loading.add(notes);for(JProgressBar bar:new JProgressBar[]{audio,bga}){bar.setStringPainted(true);bar.setUI(new javax.swing.plaf.basic.BasicProgressBarUI(){protected Color getSelectionForeground(){return new Color(0x102B2A);}protected Color getSelectionBackground(){return ui.foreground;}});bar.setForeground(new Color(0x8CD7CB));bar.setBackground(new Color(0x202C3C));bar.setFont(new Font("Yu Gothic UI",0,24));bar.setPreferredSize(new Dimension(600,46));loading.add(bar);}JPanel all=Touch.panel(new BorderLayout(0,32));all.add(text);all.add(loading,BorderLayout.SOUTH);GridBagConstraints c=new GridBagConstraints();c.weightx=1;c.fill=GridBagConstraints.HORIZONTAL;c.insets=new Insets(12,24,12,24);add(all,c);loading.setVisible(false);}
  void update(Snapshot s){
   String key=s.title+"|"+s.artist+"|"+s.genre+"|"+s.difficulty+"|"+s.table;
   if(!key.equals(previous)){previous=key;genre.setText(s.genre);title.setText(s.title.isBlank()?"Choose your next track":s.title);artist.setText(s.artist);table.setText(s.table);table.setVisible(!s.table.isBlank());}
   title.setFont(new Font("Yu Gothic UI",Font.BOLD,ui.number("titleSize",52)));try{title.setForeground(Color.decode(ui.settings.getProperty("titleColor","#8CD7CB")));}catch(Exception e){title.setForeground(ui.accent);}
   boolean scroll=false;for(MarqueeLabel label:new MarqueeLabel[]{genre,title,artist,table})scroll|=label.overflow();if(scroll&&isShowing()){if(!marquee.isRunning())marquee.start();}else marquee.stop();

   loading.setVisible(s.loading);if(s.loading){phase.setText(s.phase);notes.setText(s.notesReady?"Notes / chart: parsed":"Notes / chart: preparing…");progress(audio,"Keysounds",s.audio);progress(bga,"BGA",s.bga);}revalidate();
  }
  public void removeNotify(){marquee.stop();super.removeNotify();}
  static void progress(JProgressBar bar,String name,float fraction){int n=Math.max(0,Math.min(100,Math.round(fraction*100)));if(bar.getValue()!=n)bar.setValue(n);String text=name.toUpperCase(java.util.Locale.ROOT)+" · "+n+"%";if(!text.equals(bar.getString()))bar.setString(text);}
 }
}


