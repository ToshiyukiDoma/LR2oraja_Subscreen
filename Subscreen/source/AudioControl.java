package lr2touch;

import bms.player.beatoraja.MainController;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.*;

/** Transient menu gain. Never changes or saves the game's AudioConfig. */
public final class AudioControl implements AutoCloseable {
 static volatile AudioControl active;
 static volatile boolean hooked;
 static volatile float gain=1;
 static final float[] analog=new float[256];
 static final Map<String,Float> levels=new LinkedHashMap<>();
 final Touch ui;final javax.swing.Timer timer;final AtomicBoolean queued=new AtomicBoolean();
 volatile long lastActivity=System.nanoTime();volatile boolean allowed=true,muted,closed;
 long fadeStart;JButton mute;JDialog overlay;
 final AWTEventListener input=e->{if(e instanceof InputEvent)activity();};
 AudioControl(Touch ui){this.ui=ui;active=this;Toolkit.getDefaultToolkit().addAWTEventListener(input,AWTEvent.MOUSE_EVENT_MASK|AWTEvent.MOUSE_MOTION_EVENT_MASK|AWTEvent.MOUSE_WHEEL_EVENT_MASK|AWTEvent.KEY_EVENT_MASK);timer=new javax.swing.Timer(100,e->tick());timer.start();}
 public static void activity(){AudioControl control=active;if(control!=null&&control.allowed)control.lastActivity=System.nanoTime();}
 public static void held(boolean[] keys){AudioControl c=active;if(c==null||!c.allowed)return;for(boolean down:keys)if(down){activity();return;}}
 public static void analog(int index,boolean enabled,float value){AudioControl c=active;if(c==null||!c.allowed||!enabled||index<0||index>=analog.length)return;if(Math.abs(value-analog[index])>.01f){analog[index]=value;activity();}}
 public static float scale(String path,float volume){AudioControl control=active;if(control==null)return volume;if(path!=null){synchronized(levels){if(!levels.containsKey(path)&&levels.size()>=256)levels.remove(levels.keySet().iterator().next());levels.put(path,volume);}}return volume*gain;}
 public static void transition(Object state){AudioControl control=active;if(control==null)return;String name=state==null?"":state.getClass().getName();control.allowed=state!=null&&!name.endsWith(".BMSPlayer")&&!name.endsWith(".MusicDecide")&&!"PLAY".equals(String.valueOf(state))&&!"DECIDE".equals(String.valueOf(state));control.lastActivity=System.nanoTime();if(!control.allowed){control.muted=false;gain=1;control.restoreLevels();SwingUtilities.invokeLater(control::dismiss);}}
 JPanel controls(){JPanel row=Touch.panel(new GridLayout(1,3,14,0));mute=ui.toggleButton("Mute game",false);mute.addActionListener(e->{if(!hooked||!allowed||Touch.preview){mute.setSelected(false);mute.putClientProperty("accent",false);ui.message("MUTE IS AVAILABLE WITH THE GAME OUTSIDE CHART LOADING / PLAY");return;}if(muted)unmute();else beginMute();});row.add(mute);
  JButton timeout=ui.toggleButton("Idle mute timeout",Boolean.parseBoolean(ui.settings.getProperty("idleMute","false")));timeout.addActionListener(e->{ui.settings.setProperty("idleMute",""+timeout.isSelected());ui.save();activity();});row.add(timeout);
  int seconds=ui.number("idleMuteSeconds",30);row.add(TouchWidgets.option(ui,"Timeout duration",new String[]{"15 seconds","30 seconds","60 seconds"},seconds==15?0:seconds==60?2:1,n->{ui.settings.setProperty("idleMuteSeconds",""+new int[]{15,30,60}[n]);ui.save();activity();}));return row;
 }
 void beginMute(){if(!allowed||closed)return;muted=true;fadeStart=System.nanoTime();if(mute!=null){mute.setSelected(true);mute.putClientProperty("accent",true);mute.repaint();}}
 void tick(){if(closed||Touch.preview||!hooked||!(ui.attachedMain instanceof MainController))return;
  if(!allowed){if(muted||gain!=1)unmute();return;}
  if(!muted&&Boolean.parseBoolean(ui.settings.getProperty("idleMute","false"))&&System.nanoTime()-lastActivity>=Math.max(15,ui.number("idleMuteSeconds",30))*1_000_000_000L)beginMute();
  if(muted){float next=Math.max(0,1-(System.nanoTime()-fadeStart)/1_000_000_000f);if(next!=gain){gain=next;refresh();}if(next==0&&overlay==null)showOverlay();}
 }
 void refresh(){if(queued.compareAndSet(false,true))ui.postRaw(()->{try{restoreLevels();}finally{queued.set(false);}});}
 void restoreLevels(){if(!(ui.attachedMain instanceof MainController))return;Map<String,Float> snapshot;synchronized(levels){snapshot=new LinkedHashMap<>(levels);}for(var entry:snapshot.entrySet())((MainController)ui.attachedMain).getAudioProcessor().setVolume(entry.getKey(),entry.getValue());}
 void showOverlay(){if(!allowed||!muted)return;overlay=new JDialog(ui.frame,"GAME MUTED",false);overlay.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);overlay.addWindowListener(new WindowAdapter(){public void windowClosing(WindowEvent e){unmute();}});JButton tap=ui.button("GAME MUTED · TAP TO UNMUTE",this::unmute);overlay.add(tap);ui.themeTree(tap);overlay.setSize(Math.min(900,ui.frame.getWidth()-30),280);overlay.setLocationRelativeTo(ui.frame);overlay.setVisible(true);}
 void dismiss(){if(overlay!=null){overlay.dispose();overlay=null;}if(mute!=null){mute.setSelected(false);mute.putClientProperty("accent",false);mute.repaint();}}
 void unmute(){muted=false;gain=1;lastActivity=System.nanoTime();refresh();dismiss();ui.returnFocus();}
 public void close(){closed=true;muted=false;gain=1;timer.stop();Toolkit.getDefaultToolkit().removeAWTEventListener(input);dismiss();if(active==this)active=null;ui.postRaw(()->{restoreLevels();synchronized(levels){levels.clear();}if(active==this)active=null;});if(Touch.preview)active=null;}
}
