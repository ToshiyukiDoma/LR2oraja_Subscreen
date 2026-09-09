package lr2touch;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;

/** Event-driven companion UI. Game mutations are posted to the libGDX thread. */
public class Touch {
    static Path base;
    static Touch ui;
    static boolean preview; AudioControl audioControl;
    static final AtomicBoolean busy = new AtomicBoolean();
    boolean settingsInitialized; volatile Object attachedMain; volatile boolean concentrating; String currentPage="home"; GameMonitor monitor; GameMonitor.SongPanel songPanel; GameSettings gameSettings; Backgrounds backgrounds;
    final Properties settings = new Properties();
    JFrame frame;
    JPanel keyboard, functions, candidates, home, keys, deck;JScrollPane candidateScroll;JPanel searchPanel;
    final JTextField query = new JTextField(), composition = new JTextField();
    final JLabel reading = new JLabel("Reading"), status = new JLabel("Waiting for the game launcher");
    final JComboBox<String> mode = new JComboBox<>(new String[]{"English QWERTY", "Romaji → Hiragana", "Romaji → Katakana", "Hiragana keys", "Katakana keys"});
    final Background surface = new Background();
    boolean shift, converting, loadingBackground, dark = true;
    volatile boolean fullscreenSupport;
    int dim = 55;
    javax.swing.Timer connectTimer;
    long configuredWindow;
    int originalAutoIconify = -1;
    final Set<Integer> heldKeys = new HashSet<>(); // accessed only on game thread
    Color foreground, muted, card, fieldColor, accent, line;

    public static void premain(String args, Instrumentation inst) { inst.addTransformer(new AudioHooks());AudioControl.hooked=true;start(false); }
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--test")) { selfTest(); return; }
        start(true);
    }
    static void start(boolean p) {
        preview = p;
        try {
            base = Paths.get(Touch.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
            SwingUtilities.invokeLater(() -> { try { ui = new Touch(); } catch (Throwable e) { e.printStackTrace(); } });
        } catch (Exception e) { e.printStackTrace(); }
    }
    Touch() throws Exception {
        Files.createDirectories(base.resolve("backgrounds"));
        Path cfg = base.resolve("settings.properties");
        if (Files.exists(cfg)) try (InputStream in = Files.newInputStream(cfg)) { settings.load(in); }
        defaultStyle.putAll(settings);
        dark = Boolean.parseBoolean(settings.getProperty("dark", "true"));
        dim = Math.max(0, Math.min(100, number("dim", 55)));
        fullscreenSupport = Boolean.parseBoolean(settings.getProperty("fullscreenSupport", "false"));
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());TouchWidgets.install();
        frame = new JFrame("LR2oraja Subscreen");
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() { public void windowClosing(WindowEvent e) { if(preview)closePanel();else quitGame(); } });
        surface.setLayout(new BorderLayout(16,20));
        surface.setBorder(BorderFactory.createEmptyBorder(22,28,20,28));
        frame.setContentPane(surface);
        JPanel top = panel(new BorderLayout(10,16));
        JPanel brand = panel(new BorderLayout());
        JLabel title = new JLabel("LR2oraja Subscreen"); title.setFont(new Font("Yu Gothic UI", Font.BOLD, 27));
        title.putClientProperty("preserveCase",true);brand.add(title, BorderLayout.WEST);
        JLabel version = new JLabel("v1.0"); version.setFont(new Font("Yu Gothic UI", Font.PLAIN, 13));
        version.putClientProperty("preserveCase",true);brand.add(version, BorderLayout.EAST); top.add(brand, BorderLayout.NORTH);
        JPanel nav = panel(new GridLayout(1,7,8,0));
        nav.add(button("Home", () -> showPage("home")));
        nav.add(button("Keyboard", () -> showPage(keyboard.isVisible()?"home":"keyboard")));
        nav.add(button("Navigation", () -> showPage(functions.isVisible()?"home":"navigation")));
        nav.add(button("Play", ()->showPage("gameplay")));nav.add(button("Profiles", this::profiles));
        nav.add(button("Style", this::appearance));nav.add(button("Skins",()->{if(skinPanel==null)skinPanel=new SkinPanel(this);showContent("skins",skinPanel);skinPanel.open();}));
        top.add(nav, BorderLayout.SOUTH); surface.add(top, BorderLayout.NORTH);
        JPanel middle = panel(new BorderLayout(0,18));
        JPanel search = panel(new BorderLayout(10,8));searchPanel=search;
        search.add(label("FIND YOUR NEXT TRACK", 13, Font.BOLD), BorderLayout.NORTH);
        query.setFont(new Font("Yu Gothic UI", Font.PLAIN, 25)); query.setPreferredSize(new Dimension(300,56));
        search.add(query, BorderLayout.CENTER);
        JButton find = button("Search", this::search); find.setPreferredSize(new Dimension(180,56)); find.putClientProperty("accent", true);
        search.add(find, BorderLayout.EAST); query.addActionListener(e -> search()); query.addMouseListener(new MouseAdapter(){public void mousePressed(MouseEvent e){if(!concentrating){showPage("keyboard");query.requestFocusInWindow();}}}); middle.add(search, BorderLayout.NORTH);
        JPanel pages = panel(new BorderLayout());
        home = panel(new BorderLayout(0,18));
        JPanel welcome = panel(new GridLayout(0,1,0,14));
        homeMessage=new HomeMessage();home.add(homeMessage,BorderLayout.CENTER);refreshHomeMessage();
        JLabel welcomeText=label("Search, tune your setup, or choose a different player.",18,Font.PLAIN);welcomeText.setHorizontalAlignment(SwingConstants.CENTER);welcome.add(welcomeText);
        home.add(welcome,BorderLayout.SOUTH);
        keyboard = makeKeyboard(); functions = makeNavigation();
        deck = panel(new CardLayout()); deck.add(home,"home"); deck.add(keyboard,"keyboard"); deck.add(functions,"navigation");songPanel=new GameMonitor.SongPanel(this);deck.add(songPanel,"song");gameSettings=new GameSettings(this);deck.add(gameSettings,"gameplay");gameSettings.open("game");
        keyboard.setVisible(false); functions.setVisible(false); pages.add(deck); middle.add(pages, BorderLayout.CENTER);
        surface.add(middle, BorderLayout.CENTER);
        JPanel footer=panel(new BorderLayout()); status.setFont(new Font("Yu Gothic UI",0,14)); footer.add(status,BorderLayout.CENTER);
        JButton back=button("Return to game",this::returnFocus);back.setPreferredSize(new Dimension(240,64));footer.add(back,BorderLayout.EAST);
        surface.add(footer,BorderLayout.SOUTH);
        frame.setMinimumSize(new Dimension(960,680));
        backgrounds=new Backgrounds(this);applyTheme();
        GraphicsDevice[] screens=GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        setMonitor(Math.max(0,Math.min(number("monitor",screens.length>1?1:0),screens.length-1)),screens.length>1);
        String image=settings.getProperty("background","");
        if(!image.isEmpty())loadBackground(base.resolve("backgrounds").resolve(image));
        String video=settings.getProperty("video","");if(!video.isEmpty()){Path selected=base.resolve("backgrounds").resolve(video).normalize();if(selected.startsWith(base.resolve("backgrounds"))&&Files.isRegularFile(selected))backgrounds.play(selected);}
        if(preview) message("Preview • game commands are disconnected");
        else {
            // This discovery timer stops once the game window exists. No steady-state polling.
            monitor=new GameMonitor(this);monitor.start();
            connectTimer=new javax.swing.Timer(1000,e -> command(main -> {
                if(call(main,"getCurrentState")==null)return;
                configureFullscreen();
                SwingUtilities.invokeLater(()->connectTimer.stop());
                message("Connected • ready when you are");
            })); connectTimer.start();
        }
    }
    int number(String key,int fallback){try{return Integer.parseInt(settings.getProperty(key,""+fallback));}catch(NumberFormatException e){return fallback;}}
    static JPanel panel(LayoutManager layout){JPanel p=new JPanel(layout);p.setOpaque(false);return p;}
    static JLabel label(String text,int size,int style){JLabel l=new JLabel(text);l.setFont(new Font("Yu Gothic UI",style,Math.max(24,size)));l.setUI(new TouchWidgets.OutlineUI());return l;}
    JButton button(String text,Runnable action){JButton b=new SoftButton(text);b.setFocusable(false);b.setPreferredSize(new Dimension(120,64));b.setMinimumSize(new Dimension(0,64));b.addActionListener(e->action.run());return b;}
    JPanel makeKeyboard(){
        JPanel p=panel(new BorderLayout(0,10));p.putClientProperty("preserveCase",true);
        JPanel entry=panel(new BorderLayout(10,6));TouchWidgets.combo(mode);entry.add(mode,BorderLayout.WEST);
        composition.setFont(new Font("Yu Gothic UI",0,23));composition.setPreferredSize(new Dimension(300,48));entry.add(composition,BorderLayout.CENTER);
        entry.add(reading,BorderLayout.SOUTH);p.add(entry,BorderLayout.NORTH);
        keys=panel(new GridLayout(0,1,5,5));p.add(TouchWidgets.scroll(keys),BorderLayout.CENTER);
        JPanel bottom=panel(new BorderLayout(0,8));JPanel edit=panel(new GridLayout(1,5,8,0));
        JButton shiftKey=button("Shift",()->{shift=!shift;buildKeys(keys);}),spaceKey=button("Space",()->type(" ")),kanji=button("Convert 漢字",this::convert),kana=button("Commit kana",()->commit(readingText())),clear=button("Clear text",()->{query.setText("");composition.setText("");clearCandidates();});
        Runnable language=()->{edit.removeAll();edit.add(shiftKey);edit.add(spaceKey);if(mode.getSelectedIndex()!=0){edit.add(kanji);edit.add(kana);}edit.add(clear);edit.setLayout(new GridLayout(1,0,8,0));edit.revalidate();edit.repaint();};mode.addActionListener(e->language.run());language.run();bottom.add(edit,BorderLayout.NORTH);
        candidates=panel(new FlowLayout(FlowLayout.LEFT,6,3));JScrollPane scroll=TouchWidgets.scroll(candidates);candidateScroll=scroll;scroll.setPreferredSize(new Dimension(600,68));bottom.add(scroll,BorderLayout.SOUTH);p.add(bottom,BorderLayout.SOUTH);
        mode.addActionListener(e->{buildKeys(keys);updateReading();});composition.getDocument().addDocumentListener(new DocumentListener(){public void insertUpdate(DocumentEvent e){updateReading();}public void removeUpdate(DocumentEvent e){updateReading();}public void changedUpdate(DocumentEvent e){updateReading();}});
        buildKeys(keys);updateReading();return p;
    }
    void buildKeys(JPanel keys){
        keys.removeAll();
        String[] rows=mode.getSelectedIndex()<3?new String[]{"1 2 3 4 5 6 7 8 9 0 - Backspace","q w e r t y u i o p","a s d f g h j k l '","z x c v b n m , . /"}:new String[]{"あ い う え お か き く け こ Backspace","さ し す せ そ た ち つ て と","な に ぬ ね の は ひ ふ へ ほ","ま み む め も や ゆ よ わ を","ら り る れ ろ ん っ ゃ ゅ ょ","が ぎ ぐ げ ご ざ じ ず ぜ ぞ","だ ぢ づ で ど ば び ぶ べ ぼ","ぱ ぴ ぷ ぺ ぽ ぁ ぃ ぅ ぇ ぉ ー"};
        for(String row:rows){JPanel r=panel(new GridBagLayout());int index=0;for(String k:row.split(" ")){
            boolean back=k.equals("Backspace");String value=back?k:mode.getSelectedIndex()==4?katakana(k):shift?k.toUpperCase(Locale.ROOT):k;
            GridBagConstraints c=new GridBagConstraints();c.gridx=index++;c.weightx=back?2:1;c.weighty=1;c.fill=GridBagConstraints.BOTH;c.insets=new Insets(0,2,0,2);
            JButton b=button(value,()->{if(back)backspace(mode.getSelectedIndex()==0?query:composition);else type(value);});
            b.putClientProperty("preserveCase",true);b.setPreferredSize(new Dimension(back?125:48,48));b.setMinimumSize(new Dimension(back?100:24,32));r.add(b,c);
        }keys.add(r);} keys.setPreferredSize(new Dimension(700,rows.length*68));themeTree(keys);keys.revalidate();keys.repaint();
    }
    static void backspace(JTextField field){int pos=field.getCaretPosition();if(field.getSelectionStart()!=field.getSelectionEnd())field.replaceSelection("");else if(pos>0){int start=field.getText().offsetByCodePoints(pos,-1);field.select(start,pos);field.replaceSelection("");}}
    JPanel makeNavigation(){
        JPanel p=panel(new BorderLayout(0,14));JPanel groups=panel(new GridLayout(0,1,0,12));
        JPanel named=panel(new GridLayout(1,2,10,0));named.add(button("Key settings",()->key(54)));JButton quit=button("Quit game",this::quitGame);quit.putClientProperty("danger",true);named.add(quit);groups.add(named);
        JPanel f=panel(new GridLayout(0,4,8,8));String[] names={"Next replay","Next rival","Same folder","Chart document","FPS","Refresh folder","Open folder","Display mode","Mod menu","Screenshot","Favorite song","Favorite chart","Autoplay folder","IR page"};int[] codes={52,55,56,57,290,291,292,293,294,295,297,298,299,300};for(int i=0;i<names.length;i++){final int code=codes[i];f.add(button(names[i],()->key(code)));}groups.add(f);
        JPanel arrows=panel(new GridLayout(1,6,8,0));arrows.add(button("Left",()->key(263)));arrows.add(button("Up",()->key(265)));arrows.add(button("Down",()->key(264)));arrows.add(button("Right",()->key(262)));arrows.add(button("Enter",()->key(257)));arrows.add(button("Back",()->key(256)));groups.add(arrows);
        audioPanel=panel(new GridLayout(1,3,14,0));audioControls(50,50,50);groups.add(audioPanel);audioControl=new AudioControl(this);groups.add(audioControl.controls());TouchWidgets.stack(groups);p.add(TouchWidgets.scroll(groups),BorderLayout.CENTER);p.add(label("QUIT GAME ASKS FIRST. ESCAPE IS SENT DIRECTLY TO THE GAME.",15,0),BorderLayout.SOUTH);return p;
    }
    JPanel labeled(String title,JComponent body){JPanel p=panel(new BorderLayout(0,8));p.add(label(title,13,Font.BOLD),BorderLayout.NORTH);p.add(body);return p;}
    void showPage(String page){boolean leaving=keyboard.isVisible()&&!page.equals("keyboard");currentPage=page;if(page.equals("home"))refreshHomeMessage();searchPanel.setVisible(!concentrating&&(page.equals("home")||page.equals("keyboard")));if(page.equals("navigation"))refreshAudio();if(page.equals("gameplay"))gameSettings.open("game");if(page.equals("home")&&concentrating)page="song";((CardLayout)deck.getLayout()).show(deck,page);frame.revalidate();frame.repaint();if(leaving||page.equals("home"))returnFocus();}
    void type(String text){(mode.getSelectedIndex()==0?query:composition).replaceSelection(text);}
    String readingText(){String text=composition.getText();if(mode.getSelectedIndex()==1||mode.getSelectedIndex()==2)text=roman(text);return mode.getSelectedIndex()==2||mode.getSelectedIndex()==4?katakana(text):text;}
    void updateReading(){boolean english=mode.getSelectedIndex()==0;composition.setEnabled(!english);composition.setVisible(!english);if(candidateScroll!=null)candidateScroll.setVisible(!english);reading.setText(english?"English keys write directly into song search":"Reading: "+readingText());}
    void clearCandidates(){candidates.removeAll();candidates.revalidate();candidates.repaint();}
    void commit(String text){if(text.isEmpty())return;query.replaceSelection(text);composition.setText("");clearCandidates();}
    void convert(){
        if(converting)return;final String text=hiragana(readingText().trim());if(text.isEmpty()){message("Type a Japanese reading, then choose Convert.");return;}converting=true;message("Finding candidates…");
        new SwingWorker<java.util.List<String>,Void>(){
            protected java.util.List<String> doInBackground()throws Exception{return lookup(base.resolve("SKK-JISYO.L"),text);}
            protected void done(){try{if(!text.equals(hiragana(readingText().trim())))return;clearCandidates();for(String value:get())candidates.add(button(value,()->commit(value)));themeTree(candidates);candidates.revalidate();message("Choose a word to insert it into song search");}catch(Exception e){message("Dictionary could not be read");}finally{converting=false;}}
        }.execute();
    }
    void search(){
        if(!composition.getText().isEmpty())commit(readingText());final String text=query.getText().trim();if(text.isEmpty()||text.length()>100){message("Enter 1–100 characters to search");return;}
        command(main->{Object state=call(main,"getCurrentState");if(!isSelection(state)){message("Open song selection before searching");return;}
            Class<?> type=Class.forName("bms.player.beatoraja.select.bar.SearchWordBar");Object bar=type.getConstructor(state.getClass(),String.class).newInstance(state,text);int count=Array.getLength(call(bar,"getChildren"));
            if(count==0){message("No songs found: "+text);focusGame();return;}Object manager=call(state,"getBarManager");invoke(manager,"addSearch",bar);invoke(manager,"updateBar",bar);focusGame();message(count+" chart(s) found • results are on the game screen");
        });
    }
    static boolean isSelection(Object state){return state!=null&&state.getClass().getName().equals("bms.player.beatoraja.select.MusicSelector");}
    static boolean isPlay(Object state){return state==null||state.getClass().getName().contains(".play.BMSPlayer");}
    interface Action{void run(Object main)throws Exception;}
    void message(String text){SwingUtilities.invokeLater(()->status.setText(text));}
    void key(int code){
        command(main->{if(isPlay(call(main,"getCurrentState"))){message("Touch controls are locked during gameplay");return;}
            Object input=Class.forName("com.badlogic.gdx.Gdx").getField("input").get(null),window=field(input,"window");long handle=(Long)call(window,"getWindowHandle");
            Object callback=field(input,"keyCallback");Method fire=Class.forName("org.lwjgl.glfw.GLFWKeyCallbackI").getMethod("invoke",long.class,int.class,int.class,int.class,int.class);
            if(!heldKeys.add(code))return;configureFullscreen();focusGame();
            try{fire.invoke(callback,handle,code,0,1,0);}catch(Exception e){heldKeys.remove(code);throw e;}
            javax.swing.Timer release=new javax.swing.Timer(120,e->postRaw(()->{try{fire.invoke(callback,handle,code,0,0,0);focusGame();}catch(Exception ex){message("Key release failed; use your physical keyboard");}finally{heldKeys.remove(code);}}));release.setRepeats(false);release.start();
            message("Sent "+(code>=290&&code<=301?"F"+(code-289):code>=48&&code<=57?"system "+(code-48):"navigation key"));
        });
    }
    void postRaw(Runnable task){try{Object app=Class.forName("com.badlogic.gdx.Gdx").getField("app").get(null);if(app!=null)call(app,"postRunnable",new Class<?>[]{Runnable.class},new Object[]{task});}catch(Exception e){message("Game is not available");}}
    void returnFocus(){if(!preview)postRaw(()->{try{focusGame();}catch(Exception e){message("Start the game to return focus");}});}
    void focusGame()throws Exception{Object input=Class.forName("com.badlogic.gdx.Gdx").getField("input").get(null);long window=(Long)call(field(input,"window"),"getWindowHandle");Class.forName("org.lwjgl.glfw.GLFW").getMethod("glfwFocusWindow",long.class).invoke(null,window);}
    void configureFullscreen()throws Exception{
        Object input=Class.forName("com.badlogic.gdx.Gdx").getField("input").get(null);long handle=(Long)call(field(input,"window"),"getWindowHandle");Class<?> glfw=Class.forName("org.lwjgl.glfw.GLFW");int flag=glfw.getField("GLFW_AUTO_ICONIFY").getInt(null);
        if(configuredWindow!=handle){configuredWindow=handle;originalAutoIconify=(Integer)glfw.getMethod("glfwGetWindowAttrib",long.class,int.class).invoke(null,handle,flag);}
        glfw.getMethod("glfwSetWindowAttrib",long.class,int.class,int.class).invoke(null,handle,flag,fullscreenSupport?0:originalAutoIconify);
    }
    void quitGame(){if(!confirm("QUIT LR2ORAJA?","THIS WILL CLOSE THE ENTIRE GAME.\nQUIT NOW?")){returnFocus();return;}command(main->{call(main,"exit");SwingUtilities.invokeLater(this::disposePanel);});}
    HomeMessage homeMessage;
    static String chooseHomeMessage(Path path)throws IOException{if(!Files.isRegularFile(path))return "A LITTLE SPACE BETWEEN TRACKS.";if(Files.size(path)>1048576)throw new IOException("Message file too large");java.util.List<String> messages=new ArrayList<>();for(String line:Files.readAllLines(path,java.nio.charset.StandardCharsets.UTF_8)){line=line.replace("\uFEFF","").trim();if(!line.isEmpty())messages.add(line);}return messages.isEmpty()?"A LITTLE SPACE BETWEEN TRACKS.":messages.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(messages.size()));}
    void refreshHomeMessage(){if(homeMessage==null)return;try{homeMessage.text=chooseHomeMessage(base.resolve("message.txt"));}catch(IOException e){homeMessage.text="A LITTLE SPACE BETWEEN TRACKS.";}homeMessage.repaint();}
    class HomeMessage extends JComponent{String text="";HomeMessage(){setPreferredSize(new Dimension(600,160));}protected void paintComponent(Graphics graphics){Graphics2D g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setFont(new Font("Yu Gothic UI",Font.BOLD,30));java.util.List<String> lines=new ArrayList<>();String line="";for(String word:text.split(" +")){String next=line.isEmpty()?word:line+" "+word;if(!line.isEmpty()&&g.getFontMetrics().stringWidth(next)>getWidth()-60){lines.add(line);line=word;}else line=next;}if(!line.isEmpty())lines.add(line);int y=Math.max(30,(getHeight()-lines.size()*38)/2+30);for(String value:lines){java.awt.font.TextLayout layout=new java.awt.font.TextLayout(value,g.getFont(),g.getFontRenderContext());Shape shape=layout.getOutline(java.awt.geom.AffineTransform.getTranslateInstance((getWidth()-layout.getAdvance())/2,y));g.setColor(dark?new Color(10,20,36):Color.WHITE);g.setStroke(new BasicStroke(3));g.draw(shape);g.setColor(foreground);g.fill(shape);y+=38;}g.dispose();}}
    SkinPanel skinPanel;JPanel audioPanel;String profileId="",profileName="";final Properties defaultStyle=new Properties();static final String[] STYLE_KEYS={"dark","dim","titleSize","titleColor","background","video","fullscreenSupport"};
    Path stylePath(String id){return base.resolve("profile-styles").resolve(java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(id.getBytes(java.nio.charset.StandardCharsets.UTF_8))+".properties");}
    void profileChanged(String id,String name){if(id==null||id.isBlank())return;boolean changed=!id.equals(profileId),renamed=!java.util.Objects.equals(name,profileName);profileId=id;profileName=name==null?"":name;if(changed){for(String key:STYLE_KEYS){settings.remove(key);if(defaultStyle.containsKey(key))settings.setProperty(key,defaultStyle.getProperty(key));}Path path=stylePath(id);if(Files.isRegularFile(path))try(InputStream in=Files.newInputStream(path)){Properties saved=new Properties();saved.load(in);for(String key:STYLE_KEYS){settings.remove(key);if(saved.containsKey(key))settings.setProperty(key,saved.getProperty(key));}}catch(IOException e){message("COULD NOT LOAD PROFILE STYLE");}dark=Boolean.parseBoolean(settings.getProperty("dark","true"));dim=number("dim",55);fullscreenSupport=Boolean.parseBoolean(settings.getProperty("fullscreenSupport","false"));backgrounds.stop();if(surface.image!=null){surface.image.flush();surface.image=null;}String bg=settings.getProperty("background","");if(!bg.isEmpty())loadBackground(base.resolve("backgrounds").resolve(bg));String video=settings.getProperty("video","");if(!video.isEmpty())backgrounds.play(base.resolve("backgrounds").resolve(video));applyTheme();if(currentPage.equals("style"))appearance();}if(changed&&!preview)postRaw(()->{try{configureFullscreen();}catch(Exception ignored){}});if(renamed&&!changed&&currentPage.equals("style"))appearance();if((changed||renamed)&&Boolean.parseBoolean(settings.getProperty("exportProfile","false")))exportProfileName();}
    void exportProfileName(){if(profileName.isBlank())return;try{ProfileRestart.atomicWrite(base.resolve("current-profile.txt"),profileName.getBytes(java.nio.charset.StandardCharsets.UTF_8));}catch(IOException e){message("COULD NOT EXPORT PROFILE NAME");}}
    void audioControls(int master,int key,int bgm){audioPanel.removeAll();int[] values={master,key,bgm};String[] names={"Master","Keysound","BGM"};for(int i=0;i<3;i++){final int index=i;audioPanel.add(TouchWidgets.number(this,names[i]+" volume",values[i],0,100,"%",v->{if(!preview)postRaw(()->{if(attachedMain instanceof bms.player.beatoraja.MainController)audioValue(((bms.player.beatoraja.MainController)attachedMain).getConfig().getAudioConfig(),index,v);});}));}themeTree(audioPanel);audioPanel.revalidate();}
    static void audioValue(bms.player.beatoraja.AudioConfig audio,int channel,int value){float v=Math.max(0,Math.min(100,value))/100f;if(channel==0)audio.setSystemvolume(v);else if(channel==1)audio.setKeyvolume(v);else audio.setBgvolume(v);}
    void refreshAudio(){if(!preview)command(main->{bms.player.beatoraja.AudioConfig audio=((bms.player.beatoraja.MainController)main).getConfig().getAudioConfig();int m=Math.round(audio.getSystemvolume()*100),k=Math.round(audio.getKeyvolume()*100),b=Math.round(audio.getBgvolume()*100);SwingUtilities.invokeLater(()->audioControls(m,k,b));});}
    boolean confirm(String title,String text){return TouchWidgets.confirm(this,title,text);}
    void closePanel(){if(!confirm("Close the touch panel?","LR2oraja will keep running.\nClose this panel?")){returnFocus();return;}disposePanel();}
    void disposePanel(){if(audioControl!=null)audioControl.close();if(connectTimer!=null)connectTimer.stop();if(monitor!=null)monitor.close();TouchWidgets.uninstallDrag();if(backgrounds!=null)backgrounds.close();fullscreenSupport=false;if(!preview)postRaw(()->{try{configureFullscreen();focusGame();}catch(Exception e){}});if(surface.image!=null){surface.image.flush();surface.image=null;}frame.dispose();if(ui==this)ui=null;}
    void receive(GameMonitor.Snapshot snapshot){
        if(!frame.isDisplayable())return;if(!settingsInitialized&&attachedMain!=null){settingsInitialized=true;gameSettings.open(gameSettings.tab);}boolean changed=concentrating!=snapshot.concentration;concentrating=snapshot.concentration;
        backgrounds.pause(concentrating);searchPanel.setVisible(!concentrating&&(currentPage.equals("home")||currentPage.equals("keyboard")));profileChanged(snapshot.profileId,snapshot.profileName);songPanel.update(snapshot);gameSettings.liveDuration(snapshot.currentDuration);
        if(concentrating&&changed){returnFocus();currentPage="home";((CardLayout)deck.getLayout()).show(deck,"song");}
        else if(!concentrating&&changed){((CardLayout)deck.getLayout()).show(deck,"home");if(currentPage.equals("gameplay"))gameSettings.open(gameSettings.tab);}
        if(changed)gameSettings.open(gameSettings.tab);
    }
    void profiles(){if(preview){message("Profiles / IR are available when launched with the game");return;}command(main->{if(!isSelection(call(main,"getCurrentState"))){message("Return to song selection to change profile");return;}Object cfg=call(main,"getConfig");String playerRoot=(String)call(cfg,"getPlayerpath"),active=(String)call(cfg,"getPlayername");SwingUtilities.invokeLater(()->ProfileRestart.show(this,Paths.get(playerRoot),active));});}
    void appearance(){
        JPanel body=panel(new BorderLayout(0,18));body.add(label("STYLE FOR: "+(profileName.isBlank()?"DEFAULT / PREVIEW":profileName),24,Font.BOLD),BorderLayout.NORTH);JPanel settingsPanel=panel(new GridLayout(0,1,0,12));JButton night=toggleButton("Dark mode",dark);settingsPanel.add(night);
        JSlider shade=new JSlider(0,100,dim);shade.setMajorTickSpacing(25);shade.setPaintLabels(true);settingsPanel.add(TouchWidgets.number(this,"Background dim",dim,0,100,"%",v->{dim=v;settings.setProperty("dim",""+v);save();surface.repaint();}));
        JButton fullscreen=toggleButton("Keep fullscreen visible",fullscreenSupport);settingsPanel.add(fullscreen);
        settingsPanel.add(label("Experimental: prevents auto-minimization; behavior depends on your driver.",14,0));
        JPanel actions=panel(new GridLayout(1,3,8,0));actions.add(button("Background gallery",this::chooseBackground));actions.add(button("Remove image",()->{if(surface.image!=null)surface.image.flush();surface.image=null;backgrounds.stop();settings.remove("video");settings.remove("background");save();applyTheme();}));settingsPanel.add(actions);settingsPanel.add(TouchWidgets.number(this,"Song title size",number("titleSize",52),30,90," px",v->{settings.setProperty("titleSize",""+v);save();}));JPanel colorRow=panel(new BorderLayout(12,0));JTextField color=new JTextField(settings.getProperty("titleColor","#8CD7CB"));colorRow.add(color);colorRow.add(button("HEX",()->{TouchWidgets.text(this,"SONG TITLE COLOR · #RRGGBB",color);String value=color.getText().trim();if(!value.matches("#[0-9a-fA-F]{6}")){message("USE #RRGGBB, FOR EXAMPLE #8CD7CB");return;}settings.setProperty("titleColor",value.toUpperCase(Locale.ROOT));save();}),BorderLayout.EAST);settingsPanel.add(labeled("Song title color · RGB hex",colorRow));settingsPanel.add(monitorOptions());body.add(settingsPanel);
        night.addActionListener(e->{dark=night.isSelected();settings.setProperty("dark",""+dark);applyTheme();themeTree(body);save();});
        shade.addChangeListener(e->{dim=shade.getValue();surface.repaint();if(!shade.getValueIsAdjusting()){settings.setProperty("dim",""+dim);save();}});
        fullscreen.addActionListener(e->{fullscreenSupport=fullscreen.isSelected();settings.setProperty("fullscreenSupport",""+fullscreenSupport);save();if(!preview)postRaw(()->{try{configureFullscreen();}catch(Exception ex){message("Fullscreen support unavailable in this build");}});});
        TouchWidgets.stack(settingsPanel);themeTree(body);showContent("style",TouchWidgets.scroll(body));
    }
    void showContent(String name,JComponent content){for(Component old:deck.getComponents())if(name.equals(old.getName()))deck.remove(old);content.setName(name);deck.add(content,name);currentPage=name;searchPanel.setVisible(false);((CardLayout)deck.getLayout()).show(deck,name);themeTree(content);frame.revalidate();frame.repaint();}
    JPanel monitorOptions(){GraphicsDevice[] devices=GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();String[] names=new String[devices.length+1];for(int i=0;i<devices.length;i++){Rectangle r=devices[i].getDefaultConfiguration().getBounds();names[i]="Screen "+(i+1)+" · "+r.width+" × "+r.height;}names[devices.length]="Windowed mode";return TouchWidgets.option(this,"Display",names,frame.isUndecorated()?number("monitor",0):devices.length,n->{setMonitor(n==devices.length?0:n,n!=devices.length);save();});}
    void setMonitor(int n,boolean full){Rectangle bounds=GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[n].getDefaultConfiguration().getBounds();frame.dispose();frame.setUndecorated(full);frame.setBounds(full?bounds:new Rectangle(bounds.x+30,bounds.y+30,Math.min(1200,bounds.width-60),Math.min(900,bounds.height-60)));frame.setVisible(true);settings.setProperty("monitor",""+n);}
    void monitorMenu(){GraphicsDevice[] devices=GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();String[] names=new String[devices.length+1];for(int i=0;i<devices.length;i++){Rectangle r=devices[i].getDefaultConfiguration().getBounds();names[i]="Screen "+(i+1)+" · "+r.width+" × "+r.height;}names[devices.length]="Windowed mode";int choice=TouchWidgets.choose(this,"Monitor",names);if(choice>=0){setMonitor(choice==devices.length?0:choice,choice!=devices.length);save();}}
    void chooseBackground(){backgrounds.gallery();}
    void applyTheme(){
        foreground=new Color(dark?0xEAF0F8:0x172334);muted=new Color(dark?0xA1B0C3:0x526174);card=new Color(dark?0x202C3C:0xF5F8FC);fieldColor=new Color(dark?0x111B29:0xFFFFFF);accent=new Color(dark?0x8CD7CB:0x16776D);line=new Color(dark?0x39495F:0xCDD7E3);
        UIManager.put("Panel.background",card);UIManager.put("OptionPane.background",card);UIManager.put("OptionPane.messageForeground",foreground);UIManager.put("Label.foreground",foreground);UIManager.put("Button.font",new Font("Yu Gothic UI",0,17));UIManager.put("OptionPane.messageFont",new Font("Yu Gothic UI",0,18));
        themeTree(surface);surface.repaint();
    }
    void themeTree(Component c){
        if(foreground==null)return;if(c.getFont()!=null&&!(c instanceof SoftButton))c.setFont(c.getFont().deriveFont((float)Math.max(24,c.getFont().getSize())));
        if(c instanceof JLabel){c.setForeground(foreground);c.setBackground(card);((JLabel)c).setOpaque(false);((JLabel)c).setUI(new TouchWidgets.OutlineUI());}else if(c instanceof JTextField){JTextField t=(JTextField)c;t.setBackground(fieldColor);t.setForeground(foreground);t.setCaretColor(accent);t.setSelectionColor(new Color(dark?0x355E6A:0xC8EAE5));t.setBorder(new CompoundBorder(new LineBorder(line,1,true),BorderFactory.createEmptyBorder(8,12,8,12)));}
        else if(c instanceof JComboBox){c.setBackground(card);c.setForeground(foreground);}
        else if(c instanceof JCheckBox){TouchWidgets.check(this,(JCheckBox)c);c.setBackground(card);c.setForeground(foreground);c.setFont(new Font("Yu Gothic UI",0,24));}
        else if(c instanceof JSlider){((JSlider)c).setOpaque(false);c.setBackground(card);c.setForeground(foreground);}
        else if(c instanceof JScrollPane){((JScrollPane)c).setBorder(null);((JScrollPane)c).setOpaque(false);((JScrollPane)c).getViewport().setOpaque(false);}
        if(c instanceof Container)for(Component child:((Container)c).getComponents())themeTree(child);
    }
    JButton toggleButton(String title,boolean selected){return new SoftButton(title){ {setSelected(selected);putClientProperty("accent",selected);setPreferredSize(new Dimension(300,64));}protected void fireActionPerformed(ActionEvent e){setSelected(!isSelected());putClientProperty("accent",isSelected());repaint();super.fireActionPerformed(e);}};}
    class SoftButton extends JButton{
        private static final long serialVersionUID=1L;
        public String getText(){String text=super.getText();return TouchWidgets.caps(this,text);}
        SoftButton(String text){super(text);setContentAreaFilled(false);setBorderPainted(false);setFocusPainted(false);setFont(new Font("Yu Gothic UI",Font.PLAIN,24));setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));setMargin(new Insets(6,8,6,8));}
        protected void paintComponent(Graphics graphics){Graphics2D g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);boolean danger=Boolean.TRUE.equals(getClientProperty("danger"));boolean primary=Boolean.TRUE.equals(getClientProperty("accent"));Color bg=danger?new Color(0xBB384B):card==null?Color.GRAY:primary?accent:card;if(getModel().isPressed())bg=bg.darker();else if(getModel().isRollover())bg=dark?bg.brighter():bg.darker();g.setColor(bg);g.fillRoundRect(0,0,getWidth()-1,getHeight()-1,16,16);g.setColor(line==null?Color.GRAY:line);if(!primary)g.drawRoundRect(0,0,getWidth()-1,getHeight()-1,16,16);g.dispose();setForeground(danger?Color.WHITE:primary?(dark?new Color(0x102B2A):Color.WHITE):foreground);if(getIcon()!=null){super.paintComponent(graphics);return;}Graphics2D text=(Graphics2D)graphics.create();text.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);text.setFont(getFont());text.setColor(isEnabled()?getForeground():getForeground().darker());FontMetrics fm=text.getFontMetrics();java.util.List<String> lines=new ArrayList<>();String line="";for(String word:getText().split(" +")){String next=line.isEmpty()?word:line+" "+word;if(!line.isEmpty()&&fm.stringWidth(next)>getWidth()-12){lines.add(line);line=word;}else line=next;}lines.add(line);int h=fm.getHeight(),y=(getHeight()-lines.size()*h)/2+fm.getAscent();for(String value:lines){text.drawString(value,(getWidth()-fm.stringWidth(value))/2,y);y+=h;}text.dispose();}
    }
    class Background extends JPanel{
        private static final long serialVersionUID=1L;BufferedImage image,videoFrame;boolean videoActive;
        protected void paintComponent(Graphics graphics){super.paintComponent(graphics);Graphics2D g=(Graphics2D)graphics.create();if(videoActive&&videoFrame!=null){g.drawImage(videoFrame,0,0,getWidth(),getHeight(),null);g.setColor(new Color(0,0,0,(int)(dim*2.55)));g.fillRect(0,0,getWidth(),getHeight());g.dispose();return;}g.setPaint(new GradientPaint(0,0,new Color(dark?0x101A29:0xE8F0F7),getWidth(),getHeight(),new Color(dark?0x1D3040:0xDDECEB)));g.fillRect(0,0,getWidth(),getHeight());if(image!=null){double scale=Math.max((double)getWidth()/image.getWidth(),(double)getHeight()/image.getHeight());int w=(int)(image.getWidth()*scale),h=(int)(image.getHeight()*scale);g.drawImage(image,(getWidth()-w)/2,(getHeight()-h)/2,w,h,null);g.setColor(new Color(0,0,0,(int)(dim*2.55)));g.fillRect(0,0,getWidth(),getHeight());}g.dispose();}
    }
 static final Map<String,String> kana=new HashMap<>();
 static {String[] rows={"a i u e o|あ い う え お","ka ki ku ke ko|か き く け こ","sa shi su se so|さ し す せ そ","ta chi tsu te to|た ち つ て と","na ni nu ne no|な に ぬ ね の","ha hi fu he ho|は ひ ふ へ ほ","ma mi mu me mo|ま み む め も","ya yu yo|や ゆ よ","ra ri ru re ro|ら り る れ ろ","wa wo|わ を","ga gi gu ge go|が ぎ ぐ げ ご","za ji zu ze zo|ざ じ ず ぜ ぞ","da di du de do|だ ぢ づ で ど","ba bi bu be bo|ば び ぶ べ ぼ","pa pi pu pe po|ぱ ぴ ぷ ぺ ぽ","kya kyu kyo sha shu sho cha chu cho nya nyu nyo hya hyu hyo mya myu myo rya ryu ryo gya gyu gyo ja ju jo bya byu byo pya pyu pyo|きゃ きゅ きょ しゃ しゅ しょ ちゃ ちゅ ちょ にゃ にゅ にょ ひゃ ひゅ ひょ みゃ みゅ みょ りゃ りゅ りょ ぎゃ ぎゅ ぎょ じゃ じゅ じょ びゃ びゅ びょ ぴゃ ぴゅ ぴょ","xa xi xu xe xo xtsu xya xyu xyo va vi vu ve vo fa fi fe fo she che je ti tu si zi tya tyu tyo sya syu syo|ぁ ぃ ぅ ぇ ぉ っ ゃ ゅ ょ ゔぁ ゔぃ ゔ ゔぇ ゔぉ ふぁ ふぃ ふぇ ふぉ しぇ ちぇ じぇ てぃ とぅ し じ ちゃ ちゅ ちょ しゃ しゅ しょ"}; for(String row:rows){String[] p=row.split("\\|"),a=p[0].split(" "),b=p[1].split(" ");for(int i=0;i<a.length;i++)kana.put(a[i],b[i]);}}
 public static String roman(String s){s=s.toLowerCase(Locale.ROOT);StringBuilder out=new StringBuilder();for(int i=0;i<s.length();){char c=s.charAt(i);if(c=='n'&&(i+1==s.length()||s.charAt(i+1)=='\'')){out.append('ん');i+=i+1<s.length()?2:1;continue;}if(i+1<s.length()&&c==s.charAt(i+1)&&"bcdfghjkmprstvwxyz".indexOf(c)>=0){out.append('っ');i++;continue;}if(c=='n'&&i+1<s.length()&&"aiueoy".indexOf(s.charAt(i+1))<0){out.append('ん');i++;if(i<s.length()&&s.charAt(i)=='n'&&(i+1==s.length()||"aiueoy".indexOf(s.charAt(i+1))<0))i++;continue;}boolean found=false;for(int n=Math.min(4,s.length()-i);n>0;n--){String v=kana.get(s.substring(i,i+n));if(v!=null){out.append(v);i+=n;found=true;break;}}if(!found){out.append(c=='-'?'ー':c);i++;}}return out.toString();}
 static String katakana(String s){StringBuilder b=new StringBuilder();for(char c:s.toCharArray())b.append(c>='ぁ'&&c<='ゖ'?(char)(c+0x60):c);return b.toString();}
 static String hiragana(String s){StringBuilder b=new StringBuilder();for(char c:s.toCharArray())b.append(c>='ァ'&&c<='ヶ'?(char)(c-0x60):c);return b.toString();}
 static java.util.List<String> lookup(Path dict,String s)throws IOException{LinkedHashSet<String> list=new LinkedHashSet<>();list.add(s);list.add(katakana(s));try(BufferedReader r=Files.newBufferedReader(dict,Charset.forName("EUC-JP"))){String line;while((line=r.readLine())!=null)if(line.startsWith(s+" /")){for(String c:line.substring(s.length()+2).split("/")){c=c.split(";",2)[0];if(!c.isEmpty()&&!c.startsWith("("))list.add(c);if(list.size()>=40)break;}break;}}return new ArrayList<>(list);}

 void command(Action action){if(preview){message("Preview mode — use Launch LR2Touch.cmd for game integration.");return;}if(!busy.compareAndSet(false,true)){message("Waiting for the game to finish the previous command.");return;}try{Object app=Class.forName("com.badlogic.gdx.Gdx").getField("app").get(null);if(app==null)throw new IllegalStateException("Game has not started yet");call(app,"postRunnable",new Class<?>[]{Runnable.class},new Object[]{(Runnable)()->{try{Object listener=call(app,"getApplicationListener"),main=listener;if(!main.getClass().getName().equals("bms.player.beatoraja.MainController")){main=null;for(Field f:listener.getClass().getDeclaredFields())if(f.getType().getName().equals("bms.player.beatoraja.MainController")){f.setAccessible(true);main=f.get(listener);break;}}if(main==null)throw new IllegalStateException("Unsupported game listener");attachedMain=main;action.run(main);}catch(Throwable e){message("Command failed: "+root(e));e.printStackTrace();}finally{busy.set(false);}}});}catch(Throwable e){busy.set(false);message("Not connected: "+root(e));}}

 static String root(Throwable e){while(e.getCause()!=null)e=e.getCause();return e.toString();}
 static Object field(Object o,String n)throws Exception{for(Class<?> c=o.getClass();c!=null;c=c.getSuperclass())try{Field f=c.getDeclaredField(n);f.setAccessible(true);return f.get(o);}catch(NoSuchFieldException e){}throw new NoSuchFieldException(n);}
 static Object call(Object o,String n)throws Exception{return call(o,n,new Class<?>[0],new Object[0]);}
 static Object call(Object o,String n,Class<?>[] types,Object[] args)throws Exception{Method m=o.getClass().getMethod(n,types);m.setAccessible(true);return m.invoke(o,args);}
 static Object invoke(Object o,String name,Object arg)throws Exception{for(Method m:o.getClass().getMethods())if(m.getName().equals(name)&&m.getParameterCount()==1&&m.getParameterTypes()[0].isInstance(arg))return m.invoke(o,arg);throw new NoSuchMethodException(name);}

 void save(){try{Properties global=new Properties();global.putAll(settings);if(!profileId.isBlank()){Properties local=new Properties();for(String key:STYLE_KEYS){if(settings.containsKey(key))local.setProperty(key,settings.getProperty(key));global.remove(key);if(defaultStyle.containsKey(key))global.setProperty(key,defaultStyle.getProperty(key));}Path path=stylePath(profileId);Files.createDirectories(path.getParent());try(OutputStream out=Files.newOutputStream(path)){local.store(out,"Profile style");}}else defaultStyle.putAll(settings);try(OutputStream out=Files.newOutputStream(base.resolve("settings.properties"))){global.store(out,"Subscreen preferences");}}catch(Exception e){message("Cannot save preferences: "+e.getMessage());}}


 void loadBackground(Path p){if(loadingBackground)return;loadingBackground=true;new SwingWorker<BufferedImage,Void>(){protected BufferedImage doInBackground()throws Exception{try(ImageInputStream in=ImageIO.createImageInputStream(p.toFile())){Iterator<ImageReader> it=ImageIO.getImageReaders(in);if(!it.hasNext())throw new IOException("Unsupported image");ImageReader r=it.next();try{r.setInput(in);int w=r.getWidth(0),h=r.getHeight(0);ImageReadParam param=r.getDefaultReadParam();int sub=Math.max(1,(int)Math.ceil(Math.max(w/1920.0,h/1080.0)));param.setSourceSubsampling(sub,sub,0,0);return r.read(0,param);}finally{r.dispose();}}}protected void done(){try{BufferedImage old=surface.image;surface.image=get();if(old!=null)old.flush();applyTheme();}catch(Exception e){message("Background error: "+e.getMessage());}finally{loadingBackground=false;}}}.execute();}

 static void selfTest(){String[][] tests={{"nihongo","にほんご"},{"konnichiha","こんにちは"},{"gakkou","がっこう"},{"kan'i","かんい"},{"shinjuku","しんじゅく"},{"nyan","にゃん"},{"nn","ん"},{"kya","きゃ"}};for(String[] t:tests)if(!roman(t[0]).equals(t[1]))throw new AssertionError(t[0]+" -> "+roman(t[0]));if(!katakana("ひらがな").equals("ヒラガナ"))throw new AssertionError();System.out.println("PASS: romaji and kana conversion");}
}





