package lr2touch;
import bms.model.Mode;
import bms.player.beatoraja.*;
import bms.player.beatoraja.play.*;
import bms.player.beatoraja.select.*;
import bms.player.beatoraja.select.bar.SongBar;
import java.awt.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.swing.*;

public final class GameSettings extends JPanel {
 JLabel currentGreen; final Touch ui;String tab="game";Mode mode=Mode.BEAT_7K;boolean live,locked;final JPanel body=Touch.panel(new GridBagLayout()),global=Touch.panel(new GridLayout(1,3,14,0));JPanel destination=body;int row;
 final ConcurrentHashMap<String,Integer> pending=new ConcurrentHashMap<>();final AtomicBoolean queued=new AtomicBoolean();
 static final String[] GAUGES={"Assist Easy","Easy","Normal","Hard","EX-Hard","Hazard"},RANDOM={"Off","Mirror","Random","R-Random","S-Random","Spiral","H-Random","All-scratch","Random-EX","S-Random-EX"};
 GameSettings(Touch ui){super(new BorderLayout(0,18));this.ui=ui;setOpaque(false);add(TouchWidgets.scroll(body));}
 void addRow(Component component){GridBagConstraints c=new GridBagConstraints();c.gridx=0;c.gridy=row++;c.weightx=1;c.fill=GridBagConstraints.HORIZONTAL;c.insets=new Insets(0,0,18,0);body.add(component,c);}
 void group(Runnable contents){JPanel old=destination;destination=Touch.panel(new GridLayout(1,0,14,0));contents.run();JPanel built=destination;destination=old;addRow(built);}
 void item(Component c){if(destination==body)addRow(c);else destination.add(c);}
 static int index(Object[] values,Object current){for(int i=0;i<values.length;i++)if(java.util.Objects.equals(values[i],current))return i;return 0;}
 static String modeName(Mode m){if(m==null)return "ALL MODES";switch(m){case BEAT_7K:return "7-KEYS SINGLE";case BEAT_14K:return "14-KEYS DOUBLE";case POPN_9K:return "9-KEYS POPN";case BEAT_5K:return "5-KEYS SINGLE";case BEAT_10K:return "10-KEYS DOUBLE";case KEYBOARD_24K:return "24K KEYBOARD";case KEYBOARD_24K_DOUBLE:return "48K KEYBOARD";default:return m.hint.toUpperCase(java.util.Locale.ROOT);}}
 static String[] sortNames(){return java.util.Arrays.stream(BarSorter.allSorter).map(Enum::name).toArray(String[]::new);}
 void open(String page){tab=page;if(Touch.preview){render(sample());return;}ui.command(main->{Snapshot s=read((MainController)main);SwingUtilities.invokeLater(()->render(s));});}
 Snapshot sample(){Snapshot s=new Snapshot();s.values.put("duration",300);s.values.put("lane",200);s.values.put("lift",100);s.values.put("hidden",100);s.values.put("gauge",2);s.values.put("rate",100);return s;}
 Snapshot read(MainController main){
  Snapshot s=new Snapshot();MainState state=main.getCurrentState();PlayerResource r=main.getPlayerResource();PlayerConfig p=r.getPlayerConfig();s.live=state instanceof BMSPlayer;s.selection=state instanceof MusicSelector;
  if(s.live){mode=r.getBMSModel().getMode();for(Object c:r.getConstraint())if(c.toString().equals("NO_SPEED"))s.noSpeed=true;}
  else if(state instanceof MusicSelector){Object b=((MusicSelector)state).getBarManager().getSelected();if(b instanceof SongBar){for(Mode candidate:Mode.values())if(candidate.id==((SongBar)b).getSongData().getMode())mode=candidate;}}
  if(!s.live&&p.getMode()!=null)mode=p.getMode();if(mode==null)mode=Mode.BEAT_7K;PlayConfig pc=s.live?((BMSPlayer)state).getLanerender().getPlayConfig():p.getPlayConfig(mode).getPlayconfig();
  s.values.put("keymode",index(MusicSelector.MODE,p.getMode()));s.values.put("hold",p.getLnmode());s.values.put("sort",index(sortNames(),p.getSortid()));
  s.targets=TargetProperty.getTargets().clone();s.targetNames=java.util.Arrays.stream(s.targets).map(id->{String name=TargetProperty.getTargetName(id);return name==null||name.isBlank()?id:name;}).toArray(String[]::new);s.values.put("target",index(s.targets,p.getTargetid()));
  s.values.put("shift",p.getGaugeAutoShift());s.values.put("bottom",p.getBottomShiftableGauge());s.values.put("double",p.getDoubleoption());s.values.put("algorithm",JudgeAlgorithm.getIndex(pc.getJudgetype()));s.values.put("speedAuto",pc.isEnableHispeedAutoAdjust()?1:0);
  s.values.put("gauge",p.getGauge());s.values.put("random",p.getRandom());s.values.put("random2",p.getRandom2());s.values.put("offset",p.getJudgetiming());s.values.put("autoOffset",p.isNotesDisplayTimingAutoAdjust()?1:0);
  s.values.put("duration",pc.getDuration());s.values.put("lane",Math.round(pc.getLanecover()*1000));s.values.put("lift",Math.round(pc.getLift()*1000));s.values.put("hidden",Math.round(pc.getHidden()*1000));s.values.put("laneOn",pc.isEnablelanecover()?1:0);s.values.put("liftOn",pc.isEnablelift()?1:0);s.values.put("hiddenOn",pc.isEnablehidden()?1:0);s.values.put("fix",pc.getFixhispeed());s.values.put("constant",pc.isEnableConstant()?1:0);
  try{s.values.put("rate",bms.player.beatoraja.modmenu.FreqTrainerMenu.getFreq());s.values.put("rateOn",bms.player.beatoraja.modmenu.FreqTrainerMenu.isFreqTrainerEnabled()?1:0);s.values.put("judgeOn",bms.player.beatoraja.modmenu.JudgeTrainer.isActive()?1:0);s.values.put("judge",bms.player.beatoraja.modmenu.JudgeTrainer.getJudgeRank());}catch(LinkageError e){s.mods=false;}
  return s;
 }
 void render(Snapshot s){live=s.live;locked=s.noSpeed;body.removeAll();currentGreen=null;row=0;destination=global;global.removeAll();
  choice(s,"Key Mode","keymode",java.util.Arrays.stream(MusicSelector.MODE).map(GameSettings::modeName).toArray(String[]::new),false);
  choice(s,"Hold Mode","hold",new String[]{"LN MODE","CN MODE","HCN MODE"},false);choice(s,"Music Sorting","sort",sortNames(),false);destination=body;
  addRow(Touch.label(modeName(mode)+(live?" · live controls":" · Gameplay Options"),28,Font.BOLD));
  {addRow(global);
   JPanel duration=TouchWidgets.number(ui,"Note display duration",s.values.getOrDefault("duration",500),1,10000," ms",v->change("duration",v));
   JLabel green=Touch.label("Green number: "+s.values.getOrDefault("duration",500)*3/5,26,Font.BOLD);duration.add(green,BorderLayout.SOUTH);bindGreen(duration,green);enable(duration,enabled(s,true));addRow(duration);if(s.live){currentGreen=Touch.label("Current green number: waiting for renderer",24,0);addRow(currentGreen);}
   group(()->{choice(s,"Gauge Type","gauge",GAUGES,false);choice(s,"Target Score","target",s.targetNames,false);choice(s,"P1 Lane Options","random",RANDOM,false);choice(s,"P2 Lane Options","random2",RANDOM,false);});
   group(()->{laneRow(s,"Sudden+","laneOn","Lane cover position","lane",0,1000," / 1000",true);
   laneRow(s,"Lift","liftOn","Lift position","lift",0,1000," / 1000",true);});
   group(()->{laneRow(s,"Judge Auto Adjust","autoOffset","Judge timing value","offset",-500,500," ms",false);laneRow(s,"Hidden","hiddenOn","Hidden position","hidden",0,1000," / 1000",true);});
   addRow(Touch.label("Secondary Gameplay Options",28,Font.BOLD));
   group(()->{choice(s,"Gauge Auto Shift Type","shift",new String[]{"None","Continue","Survival to Groove","Best Clear","Select to Under"},false);choice(s,"Lowest Gauge Auto Shift","bottom",new String[]{"Assist Easy","Easy","Normal"},false);choice(s,"Judge Algorithm","algorithm",java.util.Arrays.stream(JudgeAlgorithm.values()).map(Enum::name).toArray(String[]::new),false);choice(s,"Double Play Option","double",new String[]{"Off","Flip","Battle","Battle AS"},false);});
   group(()->{choice(s,"Speed reference","fix",new String[]{"Off / manual","Start BPM","Max BPM","Main BPM","Min BPM"},false);toggle(s,"Constant scroll","constant",false);toggle(s,"HiSpeed Auto Adjust","speedAuto",false);});
  } {addRow(Touch.label("SETTINGS BELOW CAN AFFECT YOUR SUBMISSION FOR IR RANKING",24,0));if(s.mods){laneRow(s,"Rate modifier","rateOn","Playback rate","rate",50,200,"%",false);group(()->{toggle(s,"Judge trainer","judgeOn",false);choice(s,"Judge difficulty","judge",new String[]{"Easy","Normal","Hard","Very hard"},false);});}}
  if(live)addRow(Touch.label(s.noSpeed?"Course rules lock speed changes":"Other options unlock in song selection",24,0));
  GridBagConstraints filler=new GridBagConstraints();filler.gridy=row;filler.weighty=1;body.add(Touch.panel(new BorderLayout()),filler);ui.themeTree(body);ui.themeTree(global);body.revalidate();global.revalidate();body.repaint();global.repaint();
 }
 void liveDuration(int ms){if(currentGreen!=null&&ms>=0)currentGreen.setText("Current green number: "+ms*3/5+" · "+ms+" ms");}
 static void bindGreen(Component c,JLabel label){if(c instanceof JSlider){JSlider slider=(JSlider)c;slider.addChangeListener(e->label.setText("Green number: "+slider.getValue()*3/5));}else if(c instanceof Container)for(Component child:((Container)c).getComponents())bindGreen(child,label);}
 boolean enabled(Snapshot s,boolean duringPlay){return Touch.preview||s.selection||s.live&&duringPlay&&!s.noSpeed;}
 void number(Snapshot s,String title,String key,int min,int max,String suffix,boolean duringPlay){JPanel p=TouchWidgets.number(ui,title,s.values.getOrDefault(key,min),min,max,suffix,v->change(key,v));enable(p,enabled(s,duringPlay));item(p);}
 void laneRow(Snapshot s,String title,String key,String numberTitle,String numberKey,int min,int max,String suffix,boolean duringPlay){JPanel old=destination;destination=Touch.panel(new BorderLayout(8,0));JPanel rowPanel=destination;JButton toggle=ui.toggleButton(title,s.values.getOrDefault(key,0)!=0);toggle.addActionListener(e->change(key,toggle.isSelected()?1:0));toggle.setPreferredSize(new Dimension(175,64));toggle.setEnabled(enabled(s,duringPlay));rowPanel.add(toggle,BorderLayout.WEST);JPanel slider=TouchWidgets.number(ui,numberTitle,s.values.getOrDefault(numberKey,min),min,max,suffix,v->change(numberKey,v));enable(slider,enabled(s,duringPlay));rowPanel.add(slider);destination=old;item(rowPanel);}
 void toggle(Snapshot s,String title,String key,boolean duringPlay){boolean on=s.values.getOrDefault(key,0)!=0;JButton b=ui.button(title+" · "+(on?"ON":"OFF"),()->{});b.putClientProperty("accent",on);b.addActionListener(e->{boolean next=!Boolean.TRUE.equals(b.getClientProperty("accent"));b.putClientProperty("accent",next);b.setText(title+" · "+(next?"ON":"OFF"));b.repaint();change(key,next?1:0);});b.setEnabled(enabled(s,duringPlay));item(b);}
 void choice(Snapshot s,String title,String key,String[] names,boolean duringPlay){JPanel p=TouchWidgets.option(ui,title,names,s.values.getOrDefault(key,0),v->{change(key,v);});enable(p,names.length>0&&enabled(s,duringPlay));item(p);}
 static void enable(Component c,boolean v){c.setEnabled(v);if(c instanceof Container)for(Component child:((Container)c).getComponents())enable(child,v);}
 void change(String key,int value){if(Touch.preview)return;pending.put(key,value);if(queued.compareAndSet(false,true))ui.postRaw(this::flush);}
 void flush(){try{if(!(ui.attachedMain instanceof MainController))return;MainController main=(MainController)ui.attachedMain;for(String key:pending.keySet()){Integer v=pending.remove(key);if(v!=null){apply(main,mode,key,v);if(key.equals("keymode")){Snapshot updated=read(main);SwingUtilities.invokeLater(()->render(updated));}}}}catch(Exception e){ui.message("Setting unavailable in this state: "+e.getClass().getSimpleName());}finally{queued.set(false);if(!pending.isEmpty()&&queued.compareAndSet(false,true))ui.postRaw(this::flush);}}
 static void apply(MainController main,Mode requested,String key,int v)throws Exception{
  MainState state=main.getCurrentState();boolean live=state instanceof BMSPlayer;boolean laneKey=Set.of("duration","lane","lift","hidden","laneOn","liftOn","hiddenOn").contains(key);
  if(!(state instanceof MusicSelector)&&!live)throw new IllegalStateException("Not adjustable now");if(live&&!laneKey)throw new IllegalStateException("Locked during play");PlayerResource r=main.getPlayerResource();PlayerConfig p=r.getPlayerConfig();
  LaneRenderer lane=live?((BMSPlayer)state).getLanerender():null;if(live){for(Object constraint:r.getConstraint())if(constraint.toString().equals("NO_SPEED"))throw new IllegalStateException("Course speed lock");}
  PlayConfig pc=live?lane.getPlayConfig():p.getPlayConfig(requested).getPlayconfig();
  switch(key){
   case "keymode":p.setMode(MusicSelector.MODE[clamp(v,0,MusicSelector.MODE.length-1)]);((MusicSelector)state).getBarManager().updateBar();break;
   case "hold":p.setLnmode(clamp(v,0,2));((MusicSelector)state).getBarManager().updateBar();break;
   case "sort":p.setSortid(sortNames()[clamp(v,0,sortNames().length-1)]);((MusicSelector)state).getBarManager().updateBar();break;
   case "target":String[] targets=TargetProperty.getTargets();if(v<0||v>=targets.length)throw new IllegalArgumentException("Target unavailable");p.setTargetid(targets[v]);break;
   case "shift":p.setGaugeAutoShift(clamp(v,0,4));break;case "bottom":p.setBottomShiftableGauge(clamp(v,0,2));break;
   case "double":p.setDoubleoption(clamp(v,0,3));break;case "algorithm":pc.setJudgetype(JudgeAlgorithm.values()[clamp(v,0,JudgeAlgorithm.values().length-1)].name());break;
   case "speedAuto":pc.setHispeedAutoAdjust(v!=0);break;
   case "gauge":p.setGauge(clamp(v,0,5));break;case "random":p.setRandom(clamp(v,0,9));break;case "random2":p.setRandom2(clamp(v,0,9));break;
   case "offset":p.setJudgetiming(clamp(v,-500,500));break;case "autoOffset":p.setNotesDisplayTimingAutoAdjust(v!=0);break;
   case "duration":if(live)lane.setDuration(clamp(v,1,10000));else pc.setDuration(clamp(v,1,10000));break;
   case "lane":if(live)lane.setLanecover(clamp(v,0,1000)/1000f);else pc.setLanecover(clamp(v,0,1000)/1000f);break;
   case "lift":if(live)lane.setLiftRegion(clamp(v,0,1000)/1000f);else pc.setLift(clamp(v,0,1000)/1000f);break;
   case "hidden":if(live)lane.setHiddenCover(clamp(v,0,1000)/1000f);else pc.setHidden(clamp(v,0,1000)/1000f);break;
   case "laneOn":if(live){lane.setEnableLanecover(v!=0);lane.setLanecover(lane.getLanecover());}else pc.setEnablelanecover(v!=0);break;
   case "liftOn":pc.setEnablelift(v!=0);break;case "hiddenOn":if(live)lane.setEnableHidden(v!=0);else pc.setEnablehidden(v!=0);break;
   case "fix":pc.setFixhispeed(clamp(v,0,4));break;case "constant":pc.setEnableConstant(v!=0);break;
   case "rate":java.lang.reflect.Field f=bms.player.beatoraja.modmenu.FreqTrainerMenu.class.getDeclaredField("freq");f.setAccessible(true);((int[])f.get(null))[0]=clamp(v,50,200);break;
   case "rateOn":bms.player.beatoraja.modmenu.FreqTrainerMenu.FREQ_TRAINER_ENABLED.set(v!=0);break;
   case "judgeOn":bms.player.beatoraja.modmenu.JudgeTrainer.setActive(v!=0);break;case "judge":bms.player.beatoraja.modmenu.JudgeTrainer.setJudgeRank(clamp(v,0,3));break;
   default:throw new IllegalArgumentException(key);
  }
 }
 static int clamp(int v,int min,int max){return Math.max(min,Math.min(max,v));}
 static class Snapshot{String[] targets={"MAX"},targetNames={"MAX"};boolean live,noSpeed,selection=true,mods=true;Map<String,Integer> values=new HashMap<>();}
}


