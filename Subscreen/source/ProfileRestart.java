package lr2touch;

import bms.player.beatoraja.IRConfig;
import bms.player.beatoraja.ir.IRConnectionManager;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.swing.*;

/** Profile edits are applied only after the original game process has exited. */
public final class ProfileRestart {
    static final ObjectMapper JSON=new ObjectMapper();
    static final String PLAYER_FILE="config_player.json";
    static class Profile {
        final String id,name; final Path path;
        Profile(String id,String name,Path path){this.id=id;this.name=name;this.path=path;}
        public String toString(){return name+"  ["+id+"]";}
    }
    static java.util.List<Profile> listProfiles(Path root)throws IOException {
        Path real=root.toRealPath();java.util.List<Profile> profiles=new ArrayList<>();
        try(DirectoryStream<Path> dirs=Files.newDirectoryStream(real)){
            for(Path dir:dirs){Path file=dir.resolve(PLAYER_FILE);if(!Files.isRegularFile(file))continue;
                Path target=file.toRealPath();if(!target.startsWith(real))continue;
                JsonNode node=JSON.readTree(target.toFile());String id=dir.getFileName().toString();
                profiles.add(new Profile(id,node.path("name").asText(id),target));
            }
        }
        profiles.sort(Comparator.comparing(p->p.id));return profiles;
    }
    static void show(Touch ui,Path playerRoot,String current){
        try{
            java.util.List<Profile> profiles=listProfiles(playerRoot);
            if(profiles.isEmpty()){ui.message("No supported player profiles found");return;}
            
            JPanel body=Touch.panel(new java.awt.BorderLayout(0,14));body.setBorder(BorderFactory.createEmptyBorder(22,22,22,22));
            JPanel form=Touch.panel(new GridLayout(0,1,0,7));
            JComboBox<Profile> players=new JComboBox<>(profiles.toArray(new Profile[0]));players.setFont(new Font("Yu Gothic UI",0,18));
            for(Profile p:profiles)if(p.id.equals(current))players.setSelectedItem(p);
            form.add(Touch.label("PLAYER PROFILE",13,Font.BOLD));TouchWidgets.combo(players);JPanel playerRow=Touch.panel(new GridLayout(1,4,16,0){public void layoutContainer(Container parent){Insets in=parent.getInsets();int width=Math.max(0,parent.getWidth()-in.left-in.right-48),height=Math.max(0,parent.getHeight()-in.top-in.bottom);int[] units={0,2,3,4,5};for(int i=0;i<parent.getComponentCount();i++)parent.getComponent(i).setBounds(in.left+width*units[i]/5+16*i,in.top,width*units[i+1]/5-width*units[i]/5,height);}});playerRow.add(players);playerRow.add(ui.button("ADD",()->addProfile(ui,playerRoot,players)));playerRow.add(ui.button("RENAME",()->renameProfileUI(ui,players)));form.add(playerRow);JButton export=ui.toggleButton("EXPORT TO TXT",Boolean.parseBoolean(ui.settings.getProperty("exportProfile","false")));export.addActionListener(e->{ui.settings.setProperty("exportProfile",""+export.isSelected());ui.save();if(export.isSelected()){ui.exportProfileName();ui.message("STREAM TEXT: SUBSCREEN / CURRENT-PROFILE.TXT");}});playerRow.add(export);
            String[] services=IRConnectionManager.getAllAvailableIRConnectionName();
            JComboBox<String> service=new JComboBox<>(services);service.setFont(new Font("Yu Gothic UI",0,18));
            JButton edit=ui.toggleButton("Update IR login",false);
            TouchWidgets.combo(service);JPanel irRow=Touch.panel(new GridLayout(1,2,16,0));irRow.add(ui.labeled("IR",service));irRow.add(edit);form.add(irRow);
            JComboBox<String> send=new JComboBox<>(new String[]{"Always","Finished Songs","Update Scores"});TouchWidgets.combo(send);JPanel sendRow=Touch.panel(new BorderLayout(0,6));sendRow.add(Touch.label("Send to IR",24,Font.BOLD),BorderLayout.NORTH);sendRow.add(send);
            JButton rivals=ui.toggleButton("Get rival score from IR",true),imports=ui.toggleButton("Import score from IR",false);JPanel flags=Touch.panel(new GridLayout(1,3,16,0));flags.add(sendRow);flags.add(rivals);flags.add(imports);form.add(flags);boolean[] irDirty={false};send.addActionListener(e->irDirty[0]=true);rivals.addActionListener(e->irDirty[0]=true);imports.addActionListener(e->irDirty[0]=true);
            Runnable loadIR=()->{try{Profile chosen=(Profile)players.getSelectedItem();if(chosen==null)return;ObjectNode saved=readIR(chosen.path,(String)service.getSelectedItem());send.setSelectedIndex(Math.max(0,Math.min(2,saved.path("irsend").asInt(0))));rivals.setSelected(saved.path("importrival").asBoolean(true));imports.setSelected(saved.path("importscore").asBoolean(false));rivals.putClientProperty("accent",rivals.isSelected());imports.putClientProperty("accent",imports.isSelected());rivals.repaint();imports.repaint();irDirty[0]=false;}catch(IOException e){ui.message("COULD NOT READ PROFILE IR OPTIONS");}};players.addActionListener(e->loadIR.run());service.addActionListener(e->loadIR.run());loadIR.run();send.setEnabled(services.length>0);rivals.setEnabled(services.length>0);imports.setEnabled(services.length>0);

            JTextField username=new JTextField();JPasswordField password=new JPasswordField();username.setFont(new Font("Yu Gothic UI",0,20));password.setFont(new Font("Yu Gothic UI",0,20));
            JPanel credentials=Touch.panel(new GridLayout(1,2,16,0));JPanel userRow=Touch.panel(new BorderLayout(12,0));userRow.add(username);userRow.add(ui.button("Edit",()->TouchWidgets.text(ui,"User ID",username)),BorderLayout.EAST);credentials.add(ui.labeled("USER ID · BLANK KEEPS SAVED VALUE",userRow));
            JPanel passwordRow=Touch.panel(new BorderLayout(12,0));passwordRow.add(password);passwordRow.add(ui.button("Edit",()->TouchWidgets.text(ui,"Password",password)),BorderLayout.EAST);credentials.add(ui.labeled("PASSWORD · BLANK KEEPS SAVED VALUE",passwordRow));form.add(credentials);
            JLabel note=Touch.label("Other IR connections and score settings will be preserved.",14,0);form.add(note);
            Runnable enable=()->{boolean enabled=edit.isSelected()&&services.length>0;service.setEnabled(services.length>0);username.setEnabled(enabled);password.setEnabled(enabled);};
            edit.setEnabled(services.length>0);edit.addActionListener(e->enable.run());enable.run();
            players.addActionListener(e->{username.setText("");password.setText("");});
            service.addActionListener(e->{username.setText("");password.setText("");});
            Component[] fields=form.getComponents();form.removeAll();form.setLayout(new GridBagLayout());int rowIndex=0;for(Component field:fields){GridBagConstraints position=new GridBagConstraints();position.gridx=0;position.gridy=rowIndex++;position.weightx=1;position.fill=GridBagConstraints.HORIZONTAL;position.insets=new Insets(0,0,7,0);form.add(field,position);}body.add(form,BorderLayout.NORTH);
            body.add(Touch.label("Tap Edit for the full-size touch keyboard",24,0),BorderLayout.CENTER);
            JPanel buttons=Touch.panel(new GridLayout(1,2,10,0));
            buttons.add(ui.button("Cancel",()->{password.setText("");ui.showPage("home");ui.returnFocus();}));
            JButton applyButton=ui.button("Apply & restart…",()->{});
            buttons.add(applyButton);
            applyButton.addActionListener(event->{
                Profile selected=(Profile)players.getSelectedItem();
                if(!ui.confirm("Restart with this profile?","Profile change will restart the game.\nSwitch to "+selected+"?"+((edit.isSelected()||irDirty[0])?"\nThe game will use these IR settings on startup.":"")))return;
                applyButton.setEnabled(false);
                try{
                    ObjectNode ir=null;
                    if(edit.isSelected()||irDirty[0]){
                        char[] secret=password.getPassword();
                        try{ir=edit.isSelected()?updatedIR(selected.path,(String)service.getSelectedItem(),username.getText(),secret):readIR(selected.path,(String)service.getSelectedItem());ir=withIROptions(ir,(String)service.getSelectedItem(),send.getSelectedIndex(),rivals.isSelected(),imports.isSelected());}
                        finally{Arrays.fill(secret,'\0');password.setText("");}
                    }
                    final ObjectNode update=ir;
                    // Recheck state at commit time: the dialog might have been open for a while.
                    ui.command(main->{if(!Touch.isSelection(Touch.call(main,"getCurrentState"))){ui.message("Return to song selection before restarting");SwingUtilities.invokeLater(()->applyButton.setEnabled(true));return;}
                        Path request=createRequest(Touch.base,playerRoot,selected.id,update);
                        java.util.List<String> command=helperCommand(Touch.base,request);
                        ProcessBuilder helper=new ProcessBuilder(command).directory(Paths.get("").toAbsolutePath().toFile());
                        helper.environment().remove("JAVA_TOOL_OPTIONS");
                        helper.redirectErrorStream(true).redirectOutput(Touch.base.resolve("restart-helper.log").toFile());
                        Process process=helper.start();
                        if(!process.isAlive()){Files.deleteIfExists(request);throw new IOException("Restart helper did not start");}
                        new SwingWorker<Boolean,Void>() {
                            protected Boolean doInBackground() throws Exception {
                                Path ready=request.resolveSibling(request.getFileName()+".ready");
                                long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
                                while(process.isAlive()&&System.nanoTime()<deadline) {
                                    if(Files.exists(ready))return true;
                                    Thread.sleep(30);
                                }
                                return false;
                            }
                            protected void done() {
                                try {
                                    if(!get()){process.destroy();Files.deleteIfExists(request);ui.message("Restart helper was not ready; game kept running");applyButton.setEnabled(true);return;}
                                    ui.command(controller->{
                                        if(!Touch.isSelection(Touch.call(controller,"getCurrentState"))){process.destroy();Files.deleteIfExists(request);ui.message("Restart cancelled: return to song selection");SwingUtilities.invokeLater(()->applyButton.setEnabled(true));return;}
                                        SwingUtilities.invokeLater(()->ui.showPage("home"));Touch.call(controller,"exit");
                                    });
                                } catch(Exception e){process.destroy();ui.message("Restart cancelled; game kept running");applyButton.setEnabled(true);}
                            }
                        }.execute();
                    });
                }catch(Exception ex){applyButton.setEnabled(true);ui.message("Cannot prepare restart: "+ex.getClass().getSimpleName());}
            });body.add(buttons,BorderLayout.SOUTH);
            ui.themeTree(body);body.setOpaque(false);body.setBackground(ui.card);
            ui.showContent("profiles",TouchWidgets.scroll(body));
        }catch(Exception ex){ui.message("Profile setup unavailable: "+ex.getClass().getSimpleName());}
    }
    static ObjectNode readIR(Path profile,String service)throws IOException{for(JsonNode entry:JSON.readTree(profile.toFile()).path("irconfig"))if(entry.path("irname").asText().equals(service))return (ObjectNode)entry.deepCopy();return JSON.createObjectNode();}
    static ObjectNode withIROptions(ObjectNode source,String service,int send,boolean rivals,boolean imports)throws IOException{if(service==null||service.isBlank()||send<0||send>2)throw new IOException("Invalid IR options");ObjectNode result=source.deepCopy();result.put("irname",service);result.put("irsend",send);result.put("importrival",rivals);result.put("importscore",imports);return result;}
    static Profile renameProfile(Path base,Profile profile,String name,bms.player.beatoraja.PlayerConfig active)throws IOException{String clean=name.trim();if(clean.isEmpty()||clean.length()>80)throw new IOException("Use a name between 1 and 80 characters");byte[] original=Files.readAllBytes(profile.path);ObjectNode config=(ObjectNode)JSON.readTree(original);config.put("name",clean);Path backup=base.resolve("backups").resolve("rename-"+UUID.randomUUID());Files.createDirectories(backup);Files.write(backup.resolve(PLAYER_FILE),original,StandardOpenOption.CREATE_NEW);Files.writeString(backup.resolve("RESTORE.txt"),"With the game closed, restore config_player.json to:\n"+profile.path.toAbsolutePath()+"\n");atomicWrite(profile.path,JSON.writerWithDefaultPrettyPrinter().writeValueAsBytes(config));if(active!=null)active.setName(clean);return new Profile(profile.id,clean,profile.path);}
    static void renameProfileUI(Touch ui,JComboBox<Profile> players){Profile selected=(Profile)players.getSelectedItem();if(selected==null)return;JTextField name=new JTextField();name.setText(selected.name);TouchWidgets.text(ui,"RENAME PROFILE",name);if(name.getText().isBlank()||name.getText().equals(selected.name))return;String requested=name.getText();ui.command(main->{if(!Touch.isSelection(Touch.call(main,"getCurrentState"))){ui.message("RETURN TO SONG SELECTION TO RENAME A PROFILE");return;}bms.player.beatoraja.PlayerConfig active=((bms.player.beatoraja.MainController)main).getPlayerConfig();Profile renamed=renameProfile(Touch.base,selected,requested,selected.id.equals(active.getId())?active:null);SwingUtilities.invokeLater(()->{int index=players.getSelectedIndex();players.insertItemAt(renamed,index);players.removeItem(selected);players.setSelectedItem(renamed);ui.message("PROFILE RENAMED");});});}
    static Profile createProfile(Path root,String name)throws IOException{String clean=name.trim();if(clean.isEmpty()||clean.length()>80)throw new IOException("Use a name between 1 and 80 characters");Path real=root.toRealPath();String id="player_"+UUID.randomUUID().toString().replace("-","");Path directory=real.resolve(id);bms.player.beatoraja.PlayerConfig config=new bms.player.beatoraja.PlayerConfig();config.setId(id);config.setName(clean);String json=bms.player.beatoraja.PlayerConfig.getConfigJson(config);Files.createDirectory(directory);try{Files.writeString(directory.resolve(PLAYER_FILE),json,StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);}catch(IOException e){Files.deleteIfExists(directory);throw e;}return new Profile(id,clean,directory.resolve(PLAYER_FILE));}
    static void addProfile(Touch ui,Path root,JComboBox<Profile> players){JTextField name=new JTextField();TouchWidgets.text(ui,"NEW PROFILE NAME",name);if(name.getText().isBlank())return;String requested=name.getText();ui.command(main->{if(!Touch.isSelection(Touch.call(main,"getCurrentState"))){ui.message("RETURN TO SONG SELECTION TO ADD A PROFILE");return;}Profile added=createProfile(root,requested);SwingUtilities.invokeLater(()->{players.addItem(added);players.setSelectedItem(added);ui.message("PROFILE CREATED. APPLY & RESTART TO SWITCH TO IT.");});});}
    static JPanel loginKeyboard(Touch ui,JTextField username,JPasswordField password){
        JPanel keyboard=Touch.panel(new java.awt.GridLayout(0,1,4,4));
        final JTextField[] target={username};final boolean[] shift={false};
        FocusAdapter focus=new FocusAdapter(){public void focusGained(FocusEvent e){target[0]=(JTextField)e.getComponent();}};
        username.addFocusListener(focus);password.addFocusListener(focus);
        String[] rows={"1 2 3 4 5 6 7 8 9 0 - _ Backspace","q w e r t y u i o p @ .","a s d f g h j k l : /","z x c v b n m ! # $ % &","( ) [ ] { } + = ? , ; '", "* ^ ~ ` \" \\ < > |"};
        java.util.List<JButton> letterButtons=new ArrayList<>();
        for(String row:rows){JPanel r=Touch.panel(new GridBagLayout());int col=0;for(String key:row.split(" ")){JButton b=ui.button(key,()->{if(!target[0].isEnabled())return;if(key.equals("Backspace"))Touch.backspace(target[0]);else target[0].replaceSelection(shift[0]?key.toUpperCase(Locale.ROOT):key);});b.setFont(new Font("Yu Gothic UI",0,key.equals("Backspace")?12:17));b.setPreferredSize(new Dimension(45,40));if(key.matches("[a-z]"))letterButtons.add(b);GridBagConstraints cell=new GridBagConstraints();cell.gridx=col++;cell.weightx=key.equals("Backspace")?2.5:1;cell.weighty=1;cell.fill=GridBagConstraints.BOTH;cell.insets=new Insets(0,2,0,2);b.setMinimumSize(new Dimension(key.equals("Backspace")?95:24,30));b.setPreferredSize(new Dimension(key.equals("Backspace")?110:40,40));r.add(b,cell);}keyboard.add(r);}
        JPanel last=Touch.panel(new GridLayout(1,4,6,0));last.add(ui.button("Shift",()->{shift[0]=!shift[0];for(JButton b:letterButtons)b.setText(shift[0]?b.getText().toUpperCase(Locale.ROOT):b.getText().toLowerCase(Locale.ROOT));}));last.add(ui.button("Space",()->{if(target[0].isEnabled())target[0].replaceSelection(" ");}));last.add(ui.button("User ID",()->{target[0]=username;username.requestFocusInWindow();}));last.add(ui.button("Password",()->{target[0]=password;password.requestFocusInWindow();}));keyboard.add(last);return keyboard;
    }
    static ObjectNode updatedIR(Path profile,String service,String user,char[] secret)throws Exception{
        ObjectNode existing=JSON.createObjectNode();
        for(JsonNode entry:JSON.readTree(profile.toFile()).path("irconfig"))if(entry.path("irname").asText().equals(service)){existing=(ObjectNode)entry.deepCopy();break;}
        Json codec=new Json();codec.setIgnoreUnknownFields(true);codec.setOutputType(JsonWriter.OutputType.json);
        IRConfig config=codec.fromJson(IRConfig.class,existing.toString());config.setIrname(service);
        if(!user.isEmpty())config.setUserid(user);
        if(secret.length>0)config.setPassword(new String(secret));
        if(!config.validate())throw new IOException("Unavailable IR service");
        JsonNode serialized=JSON.readTree(codec.toJson(config));
        // Copy credential fields only; preserve unknown connection-specific settings.
        for(String key:new String[]{"irname","userid","password","cuserid","cpassword"}){
            if(serialized.has(key))existing.set(key,serialized.get(key));else existing.remove(key);
        }
        if(!existing.path("userid").asText("").isEmpty()||!existing.path("password").asText("").isEmpty())throw new IOException("IR credentials were not encoded");
        return existing;
    }
    static Path createRequest(Path base,Path playerRoot,String id,ObjectNode ir)throws IOException{
        Path game=Paths.get("").toAbsolutePath().normalize();Path root=playerRoot.toRealPath();
        Path target=root.resolve(id).resolve(PLAYER_FILE).normalize().toRealPath();
        if(!target.startsWith(root)||!target.getParent().getFileName().toString().equals(id))throw new IOException("Invalid profile path");
        ObjectNode request=JSON.createObjectNode();request.put("pid",ProcessHandle.current().pid());
        request.put("parentStart",ProcessHandle.current().info().startInstant().orElseThrow().toString());
        request.put("game",game.toString());request.put("classPath",System.getProperty("java.class.path"));request.put("playerRoot",root.toString());request.put("profile",id);
        if(ir!=null)request.set("ir",ir);
        ArrayNode args=request.putArray("vmArgs");
        for(String arg:ManagementFactory.getRuntimeMXBean().getInputArguments())if(arg.startsWith("-Xmx")||arg.startsWith("-Xms")||arg.startsWith("-XX:")||arg.startsWith("--add-")||arg.startsWith("-D"))args.add(arg);
        Path file=base.resolve("restart-"+UUID.randomUUID()+".json");Files.writeString(file,request.toString(),StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);return file;
    }
    static java.util.List<String> helperCommand(Path base,Path request){
        return Arrays.asList(Paths.get(System.getProperty("java.home"),"bin","javaw.exe").toString(),"-cp",base.resolve("lr2touch.jar")+File.pathSeparator+Paths.get("beatoraja.jar").toAbsolutePath(),ProfileRestart.class.getName(),request.toAbsolutePath().toString());
    }
    public static void main(String[] args){
        Path request=args.length==1?Paths.get(args[0]):null;
        try{
            if(request==null)throw new IOException("No restart request");
            JsonNode job=JSON.readTree(request.toFile());long pid=job.path("pid").asLong();
            Optional<ProcessHandle> parent=ProcessHandle.of(pid);
            if(parent.isPresent()){
                Instant start=parent.get().info().startInstant().orElseThrow();
                if(!start.toString().equals(job.path("parentStart").asText()))throw new IOException("Parent process changed");
                Files.writeString(request.resolveSibling(request.getFileName()+".ready"),"ready",StandardOpenOption.CREATE_NEW);
                parent.get().onExit().get(120,TimeUnit.SECONDS);
            }
            apply(job,request.getParent());
            Path game=Paths.get(job.path("game").asText());java.util.List<String> launch=new ArrayList<>();
            launch.add(Paths.get(System.getProperty("java.home"),"bin","javaw.exe").toString());
            for(JsonNode arg:job.path("vmArgs"))launch.add(arg.asText());
            launch.add("-javaagent:"+request.getParent().resolve("lr2touch.jar").toAbsolutePath());launch.add("-cp");launch.add(job.path("classPath").asText(game.resolve("beatoraja.jar").toString()));launch.add("bms.player.beatoraja.MainLoader");launch.add("-s");
            ProcessBuilder builder=new ProcessBuilder(launch).directory(game.toFile());builder.environment().remove("JAVA_TOOL_OPTIONS");
            builder.redirectErrorStream(true).redirectOutput(request.getParent().resolve("restarted-game.log").toFile());builder.start();
            Files.deleteIfExists(request.resolveSibling(request.getFileName()+".ready"));Files.deleteIfExists(request);System.out.println("Profile saved; game restart launched.");
        }catch(Exception ex){
            System.err.println("Restart failed: "+ex.getClass().getSimpleName()+". The game was not forcibly terminated. See README recovery instructions.");
            if(request!=null)try{Files.deleteIfExists(request.resolveSibling(request.getFileName()+".ready"));Files.deleteIfExists(request);}catch(IOException ignored){}
        }
    }
    /** Minimal transactional config edits; no PlayerConfig validation or unrelated reserialization. */
    static void apply(JsonNode job,Path base)throws IOException{
        Path game=Paths.get(job.path("game").asText()).toRealPath();Path root=Paths.get(job.path("playerRoot").asText()).toRealPath();String id=job.path("profile").asText();
        if(id.isEmpty()||id.equals(".")||id.equals("..")||id.contains("/")||id.contains("\\"))throw new IOException("Invalid profile ID");
        Path profile=root.resolve(id).resolve(PLAYER_FILE).toRealPath();if(!profile.startsWith(root))throw new IOException("Profile outside root");
        Path config=game.resolve("config_sys.json");
        ObjectNode system=(ObjectNode)JSON.readTree(config.toFile());ObjectNode player=(ObjectNode)JSON.readTree(profile.toFile());
        system.put("playername",id);
        if(job.has("ir")){
            ObjectNode replacement=(ObjectNode)job.get("ir");String service=replacement.path("irname").asText();if(service.isEmpty())throw new IOException("Missing IR name");
            ArrayNode entries=player.has("irconfig")&&player.get("irconfig").isArray()?(ArrayNode)player.get("irconfig"):JSON.createArrayNode();
            boolean found=false;for(int i=0;i<entries.size();i++)if(entries.get(i).path("irname").asText().equals(service)){entries.set(i,replacement);found=true;break;}
            if(!found)entries.add(replacement);player.set("irconfig",entries);
        }
        Path backups=base.resolve("backups").resolve(System.currentTimeMillis()+"-"+UUID.randomUUID());Files.createDirectories(backups);
        Files.copy(config,backups.resolve("config_sys.json"));Files.copy(profile,backups.resolve("config_player.json"));
        Files.writeString(backups.resolve("RESTORE.txt"),"With the game closed, restore config_sys.json to:\n"+config+"\nRestore config_player.json to:\n"+profile+"\n",StandardCharsets.UTF_8);
        boolean wroteProfile=false;
        try{
            if(job.has("ir")){atomicWrite(profile,JSON.writerWithDefaultPrettyPrinter().writeValueAsBytes(player));wroteProfile=true;}
            atomicWrite(config,JSON.writerWithDefaultPrettyPrinter().writeValueAsBytes(system));
        }catch(IOException ex){if(wroteProfile)atomicWrite(profile,Files.readAllBytes(backups.resolve("config_player.json")));throw ex;}
    }
    static void atomicWrite(Path target,byte[] data)throws IOException{
        Path temp=Files.createTempFile(target.getParent(),"lr2touch-",".tmp");
        try{Files.write(temp,data);try{Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING);}}finally{Files.deleteIfExists(temp);}
    }
}

