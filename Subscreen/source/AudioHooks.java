package lr2touch;

import java.lang.instrument.*;
import java.security.ProtectionDomain;
import org.objectweb.asm.*;

/** Narrow hooks: menu sound gain, input activity, and restoration before screen loading. */
public final class AudioHooks implements ClassFileTransformer {
 public byte[] transform(ClassLoader loader,String name,Class<?> type,ProtectionDomain domain,byte[] bytes){
  if(!name.equals("bms/player/beatoraja/audio/AbstractAudioDriver")&&!name.equals("bms/player/beatoraja/MainController")&&!name.equals("bms/player/beatoraja/input/KeyBoardInputProcesseor")&&!name.equals("bms/player/beatoraja/input/BMSPlayerInputProcessor"))return null;
  try{ClassReader reader=new ClassReader(bytes);ClassWriter writer=new ClassWriter(reader,ClassWriter.COMPUTE_MAXS);
   reader.accept(new ClassVisitor(Opcodes.ASM9,writer){public MethodVisitor visitMethod(int access,String method,String desc,String signature,String[] exceptions){MethodVisitor target=super.visitMethod(access,method,desc,signature,exceptions);return new MethodVisitor(Opcodes.ASM9,target){
    void call(String method,String desc){visitMethodInsn(Opcodes.INVOKESTATIC,"lr2touch/AudioControl",method,desc,false);}
    public void visitCode(){super.visitCode();
     if(name.endsWith("/BMSPlayerInputProcessor")&&method.equals("setAnalogState")&&desc.equals("(IZF)V")){visitVarInsn(Opcodes.ILOAD,1);visitVarInsn(Opcodes.ILOAD,2);visitVarInsn(Opcodes.FLOAD,3);call("analog","(IZF)V");}

     if(name.endsWith("/AbstractAudioDriver")&&((method.equals("play")&&desc.equals("(Ljava/lang/String;FZ)V"))||(method.equals("setVolume")&&desc.equals("(Ljava/lang/String;F)V")))){visitVarInsn(Opcodes.ALOAD,1);visitVarInsn(Opcodes.FLOAD,2);call("scale","(Ljava/lang/String;F)F");visitVarInsn(Opcodes.FSTORE,2);}
     if(name.endsWith("/MainController")&&method.equals("changeState")&&(desc.equals("(Lbms/player/beatoraja/MainState;)V")||desc.equals("(Lbms/player/beatoraja/MainState$MainStateType;)V"))){visitVarInsn(Opcodes.ALOAD,1);call("transition","(Ljava/lang/Object;)V");}
     if(name.endsWith("/BMSPlayerInputProcessor")&&(method.equals("keyChanged")||method.equals("startChanged")||method.equals("setSelectPressed")||method.equals("setMousePressed")||method.equals("setMouseDragged")))call("activity","()V");
     if(name.endsWith("/KeyBoardInputProcesseor")&&(method.equals("keyDown")||method.equals("keyUp")||method.equals("keyTyped")||method.equals("mouseMoved")||method.equals("scrolled")))call("activity","()V");
    }
    public void visitInsn(int opcode){if(method.equals("poll")&&opcode==Opcodes.RETURN){visitVarInsn(Opcodes.ALOAD,0);visitFieldInsn(Opcodes.GETFIELD,name,"keystate","[Z");call("held","([Z)V");}super.visitInsn(opcode);if(name.endsWith("/KeyBoardInputProcesseor")&&method.equals("poll")&&opcode==Opcodes.BASTORE)call("activity","()V");}
   };}},0);return writer.toByteArray();
  }catch(Throwable failure){System.err.println("Subscreen optional audio hook failed: "+name);return null;}
 }
}
