package org.dishanhai.vmdiecore.monster.client;

import org.dishanhai.vmdiecore.monster.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Warm field dossier with physical identity cards and a large, scrollable briefing. */
public final class ArchiveScreen extends Screen {
    private static final int BACK=0xFF171914,PANEL=0xFF22251D,LINE=0xFF484B3B;
    private static final int PAPER=0xFFD6CBB1,PAPER_LIGHT=0xFFE2D8C1,INK=0xFF38382B;
    private static final int WHITE=0xFFF0E9D8,MUTED=0xFFB8B6A2,AMBER=0xFFE6AA55,RUST=0xFFDB8771;
    private static final String[] ROLE={"monsterperspective.ui.role.zombie","monsterperspective.ui.role.skeleton","monsterperspective.ui.role.enderman","monsterperspective.ui.role.arthropod","monsterperspective.ui.role.vampire"};
    private static final String[] WARNING={"monsterperspective.ui.warning.zombie","monsterperspective.ui.warning.skeleton","monsterperspective.ui.warning.enderman","monsterperspective.ui.warning.arthropod","monsterperspective.ui.warning.vampire"};
    private static final int[][] RISKS={{1,2,3},{1,2,4},{1},{2},{1}};
    private Race selected=Race.ZOMBIE;
    private LivingEntity preview;
    private boolean confirm,compact,initialized,draggingScroll;
    private int tab,scroll;
    private float uiScale=1;
    private int left,top,pw,ph,raceY,raceW,raceH,bodyY,bodyBottom,heroW,detailX,detailW;
    private int contentY,contentBottom,contentHeight,buttonX,buttonY,buttonW,buttonH;

    public ArchiveScreen(){super(Component.translatable("monsterperspective.ui.title"));}
    @Override protected void init(){
        // Text has a lower scale bound. Small windows scroll instead of shrinking the entire briefing.
        uiScale=Mth.clamp(Math.min((width-16)/920F,(height-16)/540F),0.75F,1.1F);
        int vw=(int)(width/uiScale),vh=(int)(height/uiScale);
        pw=Math.min(920,vw-20);ph=Math.min(540,vh-20);left=(vw-pw)/2;top=(vh-ph)/2;
        compact=ph<420;
        raceY=top+(compact?49:64);raceH=compact?36:44;raceW=(pw-32)/5;
        bodyY=raceY+raceH+12;bodyBottom=top+ph-(compact?56:72);
        heroW=Math.max(154,Math.min(326,(int)((pw-48)*0.365F)));
        detailX=left+heroW+34;detailW=left+pw-18-detailX;
        contentY=bodyY+(compact?60:78);contentBottom=bodyBottom-6;
        buttonW=compact?184:224;buttonH=compact?34:40;
        buttonX=left+pw-buttonW-16;buttonY=top+ph-buttonH-12;
        if(!initialized && minecraft.player!=null && Race.of(minecraft.player)!=null)selected=Race.of(minecraft.player);
        initialized=true;updatePreview();
    }
    private void updatePreview(){preview=selected.model==null?minecraft.player:selected.model.create(minecraft.level);scroll=0;confirm=false;}
    private static String tr(String key){return Component.translatable(key).getString();}
    private static boolean hit(double x,double y,int bx,int by,int w,int h){return x>=bx && x<bx+w && y>=by && y<by+h;}
    private void text(GuiGraphics g,String s,float x,float y,float size,int color){
        g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(size/9F,size/9F,1);g.drawString(font,s,0,0,color,false);g.pose().popPose();
    }
    private void text(GuiGraphics g,FormattedCharSequence s,float x,float y,float size,int color){
        g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(size/9F,size/9F,1);g.drawString(font,s,0,0,color,false);g.pose().popPose();
    }
    private float textWidth(String s,float size){return font.width(s)*size/9F;}
    private void right(GuiGraphics g,String s,int x,int y,float size,int color){text(g,s,x-textWidth(s,size),y,size,color);}
    private void rule(GuiGraphics g,int x,int y,int w,int color){g.fill(x,y,x+w,y+1,color);}
    private void cutPanel(GuiGraphics g,int x,int y,int w,int h,int color){
        g.fill(x+5,y,x+w,y+h-5,color);g.fill(x,y+5,x+w-5,y+h,color);
        for(int i=0;i<5;i++){g.fill(x+4-i,y+i,x+5,y+i+1,color);g.fill(x+w-5,y+h-5+i,x+w-i,y+h-4+i,color);}
    }
    private void corner(GuiGraphics g,int x,int y,int sx,int sy,int color){
        g.fill(Math.min(x,x+sx*17),y,Math.max(x,x+sx*17)+1,y+1,color);g.fill(x,Math.min(y,y+sy*17),x+1,Math.max(y,y+sy*17)+1,color);
    }
    private void plus(GuiGraphics g,int x,int y,int color){g.fill(x-3,y,x+4,y+1,color);g.fill(x,y-3,x+1,y+4,color);}
    private boolean risk(int i){for(int r:RISKS[selected.ordinal()])if(r==i)return true;return false;}
    private Race current(){return minecraft.player==null?null:Race.of(minecraft.player);}
    private int maxScroll(){return Math.max(0,contentHeight-(contentBottom-contentY));}
    private void moveScroll(int amount){scroll=Mth.clamp(scroll+amount,0,maxScroll());}
    private void scrollToPointer(double y){
        int visible=Math.max(1,contentBottom-contentY),thumb=Math.max(18,visible*visible/Math.max(1,contentHeight));
        scroll=Mth.clamp((int)(((y/uiScale-contentY-thumb/2F)/Math.max(1,visible-thumb))*maxScroll()),0,maxScroll());
    }

    @Override public void render(GuiGraphics g,int mx,int my,float delta){
        g.fillGradient(0,0,width,height,0xF311140E,0xFA201B13);
        g.pose().pushPose();g.pose().scale(uiScale,uiScale,1);mx=(int)(mx/uiScale);my=(int)(my/uiScale);
        g.fill(left+5,top+6,left+pw+5,top+ph+6,0x77000000);g.fill(left,top,left+pw,top+ph,BACK);g.renderOutline(left,top,pw,ph,LINE);
        g.fill(left,top,left+5,top+(compact?38:51),AMBER);
        if(!compact)text(g,tr("monsterperspective.ui.header"),left+18,top+12,12,MUTED);
        text(g,tr("monsterperspective.ui.identity"),left+18,top+(compact?13:30),compact?25:28,WHITE);
        if(pw>690){right(g,tr("monsterperspective.ui.personnel"),left+pw-52,top+15,12,MUTED);right(g,tr("monsterperspective.ui.tagline"),left+pw-52,top+35,13,AMBER);}
        int closeX=left+pw-36,closeY=top+13;
        if(hit(mx,my,closeX,closeY,25,25))g.fill(closeX,closeY,closeX+25,closeY+25,LINE);text(g,"×",closeX+6,closeY+4,18,MUTED);
        for(Race r:Race.values()){
            int x=left+16+r.ordinal()*raceW;boolean active=selected==r,hover=hit(mx,my,x,raceY,raceW-5,raceH);
            cutPanel(g,x,raceY,raceW-5,raceH,active?AMBER:hover?0xFF3B4030:PANEL);
            int fg=active?INK:WHITE;
            if(!compact)text(g,String.format("0%d",r.ordinal()+1),x+10,raceY+15,12,active?0xFF64502F:MUTED);
            float size=compact?14:17,tx=compact?x+(raceW-5-textWidth(tr(r.title),size))/2:x+36;
            text(g,tr(r.title),tx,raceY+(compact?11:13),size,fg);
            if(current()==r)g.fill(x+raceW-14,raceY+5,x+raceW-9,raceY+10,active?INK:AMBER);
            if(active)rule(g,x+8,raceY+raceH-4,raceW-21,INK);
        }
        drawHero(g,mx,my);drawBriefing(g,mx,my);drawFooter(g,mx,my);g.pose().popPose();
    }
    private void drawHero(GuiGraphics g,int mx,int my){
        int x=left+16,w=heroW,h=bodyBottom-bodyY;
        g.fillGradient(x,bodyY,x+w,bodyBottom,PAPER_LIGHT,PAPER);
        for(int i=0;i<42;i++){
            int px=x+5+(i*71+13)%Math.max(1,w-10),py=bodyY+4+(i*43+7)%Math.max(1,h-8);g.fill(px,py,px+2+(i%8),py+1,0x0D55462F);
        }
        corner(g,x+8,bodyY+8,1,1,0xFF93846A);corner(g,x+w-9,bodyBottom-9,-1,-1,0xFF93846A);
        text(g,tr("monsterperspective.ui.dossier"),x+16,bodyY+12,compact?12:13,INK);
        if(!compact)right(g,Component.translatable("monsterperspective.ui.record_number",String.format("%02d",selected.ordinal()+1)).getString(),x+w-16,bodyY+13,12,0xFF73684E);
        int nameY=bodyY+(compact?32:39);text(g,tr(selected.title),x+16,nameY,compact?22:30,INK);
        if(!compact)text(g,tr(ROLE[selected.ordinal()]),x+17,nameY+38,14,0xFF75633E);
        int floor=bodyBottom-(compact?25:53),ceiling=bodyY+(compact?61:99),cx=x+w/2,modelH=Math.max(25,floor-ceiling);
        g.fill(cx,ceiling+5,cx+1,floor,0x23756A50);
        for(int y=ceiling+8;y<floor;y+=20){g.fill(x+13,y,x+18,y+1,0x577E7157);g.fill(x+w-18,y,x+w-13,y+1,0x577E7157);}
        plus(g,cx,ceiling+modelH/2,0x44786B50);
        for(int i=0;i<4;i++)g.fill(cx-w/4-i*5,floor+3+i,cx+w/4+i*5,floor+4+i,(0x22-i*5)<<24|0x514932);
        if(preview!=null){
            float bodyHeight=Math.max(0.8F,preview.getBbHeight());
            int scale=Math.max(12,(int)Math.min(modelH/bodyHeight,(w-36)/Math.max(1.7F,preview.getBbWidth()*1.65F)));
            if(selected==Race.ARTHROPOD)scale=Math.min(scale,(int)((w-28)/2.8F));
            renderPreview(g,cx,floor-(int)((bodyHeight/2+0.06F)*scale),scale,mx,my);
        }
        String caption=selected==Race.VAMPIRE?tr("monsterperspective.ui.player_caption"):tr("monsterperspective.ui.monster_caption");
        if(!compact){rule(g,x+16,bodyBottom-34,w-32,0xFFAB9C7F);text(g,caption,x+16,bodyBottom-24,12,INK);}
        else text(g,selected==Race.VAMPIRE?tr("monsterperspective.ui.player_caption_short"):tr("monsterperspective.ui.monster_caption_short"),x+14,bodyBottom-17,12,INK);
    }
    private void drawBriefing(GuiGraphics g,int mx,int my){
        int x=detailX,w=detailW; text(g,tr(selected.summary),x,bodyY+3,compact?15:19,AMBER);
        int tabY;
        if(!compact){text(g,tr(selected.story),x,bodyY+32,14,MUTED);tabY=bodyY+54;}else tabY=bodyY+30;
        int tabH=compact?25:30,tw=(w-56)/2;
        for(int i=0;i<2;i++){
            int tx=x+i*(tw+8);boolean active=tab==i,hover=hit(mx,my,tx,tabY,tw,tabH);
            g.fill(tx,tabY,tx+tw,tabY+tabH,active?0xFF454B36:hover?0xFF34392C:PANEL);
            text(g,i==0?tr("monsterperspective.ui.traits_tab"):tr("monsterperspective.ui.technology_tab"),tx+12,tabY+7,compact?13:15,active?WHITE:MUTED);
            if(active)g.fill(tx,tabY+tabH-2,tx+tw,tabY+tabH,AMBER);
        }
        for(int i=0;i<2;i++){
            int ax=x+w-42+i*22;boolean hover=hit(mx,my,ax,tabY,20,tabH);
            g.fill(ax,tabY,ax+20,tabY+tabH,hover?LINE:PANEL);
            text(g,i==0?"↑":"↓",ax+4,tabY+7,15,AMBER);
        }
        contentY=tabY+tabH+12;contentBottom=bodyBottom-6;
        int textX=x+27,textW=w-42;float size=compact?15:16,lineH=compact?21:24;int y=contentY-scroll;
        String[] lines=tab==0?selected.traits:selected.technology;
        g.enableScissor((int)(x*uiScale),(int)(contentY*uiScale),(int)Math.ceil((x+w)*uiScale),(int)Math.ceil(contentBottom*uiScale));
        for(int i=0;i<lines.length;i++){
            boolean danger=tab==0 && risk(i);String label=danger?"!":String.format("%02d",i+1);
            text(g,label,x+2,y+2,danger?16:11,danger?RUST:0xFF9B9C7D);
            var wrapped=font.split(Component.translatable(lines[i]),Math.max(16,(int)(textW*9F/size)));
            for(var line:wrapped){text(g,line,textX,y,size,danger?0xFFE7BEA7:WHITE);y+=(int)lineH;}
            y+=compact?12:14;rule(g,textX,y-6,textW,0xFF303529);
        }
        contentHeight=y-contentY+scroll;g.disableScissor();
        int visible=Math.max(1,contentBottom-contentY),maxScroll=Math.max(0,contentHeight-visible);scroll=Mth.clamp(scroll,0,maxScroll);
        if(maxScroll>0){
            g.fill(x+w-4,contentY,x+w-1,contentBottom,LINE);int thumb=Math.max(18,visible*visible/Math.max(1,contentHeight)),thumbY=contentY+scroll*(visible-thumb)/maxScroll;
            g.fill(x+w-4,thumbY,x+w-1,thumbY+thumb,AMBER);
            if(scroll<maxScroll)g.fillGradient(textX,contentBottom-13,x+w-6,contentBottom,0x00171914,BACK);
        }
    }
    private void drawFooter(GuiGraphics g,int mx,int my){
        int y=top+ph-(compact?49:62);rule(g,left+16,y,pw-32,LINE);
        Race current=current();int available=buttonX-(left+18)-16;
        String status=current==null?tr("monsterperspective.ui.unregistered"):tr("monsterperspective.ui.current_identity")+tr(current.title);text(g,status,left+18,y+10,compact?13:15,WHITE);
        if(!compact){
            String warning=tr(WARNING[selected.ordinal()]),fit=font.plainSubstrByWidth(warning,Math.max(10,(int)((available-18)*9F/12)));
            text(g,"!",left+18,y+35,13,AMBER);text(g,fit,left+34,y+35,12,MUTED);
        }
        boolean same=current==selected,hover=hit(mx,my,buttonX,buttonY,buttonW,buttonH);
        cutPanel(g,buttonX,buttonY,buttonW,buttonH,same?0xFF353C2B:hover?0xFFF0BE76:AMBER);
        String label=same?tr("monsterperspective.ui.registered"):confirm?tr("monsterperspective.ui.confirm_again"):tr("monsterperspective.ui.confirm");float size=compact?14:17;
        text(g,label,buttonX+(buttonW-textWidth(label,size))/2,buttonY+(buttonH-size)/2+1,size,same?MUTED:INK);
    }
    private void renderPreview(GuiGraphics g,int cx,int cy,int scale,int mx,int my){
        float yaw=preview.getYRot(),pitch=preview.getXRot(),body=preview.yBodyRot,head=preview.yHeadRot,headOld=preview.yHeadRotO;
        float angle=(float)Math.atan((cx-mx)/120F),tilt=(float)Math.atan((cy-my)/120F);
        preview.yBodyRot=180+angle*13;preview.setYRot(180+angle*25);preview.setXRot(-tilt*8);preview.yHeadRot=preview.getYRot();preview.yHeadRotO=preview.getYRot();
        var rotation=new org.joml.Quaternionf().rotateZ((float)Math.PI);var camera=new org.joml.Quaternionf().rotateX(tilt*0.12F);rotation.mul(camera);
        preview.getPersistentData().putBoolean("mp_preview",true);
        try{InventoryScreen.renderEntityInInventory(g,cx,cy,scale,new org.joml.Vector3f(0,preview.getBbHeight()/2+0.06F,0),rotation,camera,preview);}
        finally{preview.getPersistentData().remove("mp_preview");preview.setYRot(yaw);preview.setXRot(pitch);preview.yBodyRot=body;preview.yHeadRot=head;preview.yHeadRotO=headOld;}
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        if(button!=0)return super.mouseClicked(x,y,button);x/=uiScale;y/=uiScale;
        if(hit(x,y,left+pw-36,top+13,25,25)){onClose();return true;}
        for(Race r:Race.values())if(hit(x,y,left+16+r.ordinal()*raceW,raceY,raceW-5,raceH)){selected=r;updatePreview();return true;}
        int tabY=bodyY+(compact?30:54),tabH=compact?25:30,tw=(detailW-56)/2;
        for(int i=0;i<2;i++)if(hit(x,y,detailX+i*(tw+8),tabY,tw,tabH)){tab=i;scroll=0;return true;}
        for(int i=0;i<2;i++)if(hit(x,y,detailX+detailW-42+i*22,tabY,20,tabH)){moveScroll(i==0?-80:80);return true;}
        if(maxScroll()>0 && hit(x,y,detailX+detailW-9,contentY,14,contentBottom-contentY)){draggingScroll=true;scrollToPointer(y*uiScale);return true;}
        if(hit(x,y,buttonX,buttonY,buttonW,buttonH) && minecraft.player!=null && current()!=selected){
            if(confirm){PacketDistributor.sendToServer(new Network.Choose(selected.id()));confirm=false;}else confirm=true;return true;
        }
        return super.mouseClicked(x*uiScale,y*uiScale,button);
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){
        if(hit(x/uiScale,y/uiScale,detailX,bodyY,detailW,bodyBottom-bodyY)){moveScroll(-(int)(vertical*(compact?38:48)));return true;}
        return super.mouseScrolled(x,y,horizontal,vertical);
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){
        if(draggingScroll && button==0){scrollToPointer(y);return true;}return super.mouseDragged(x,y,button,dx,dy);
    }
    @Override public boolean mouseReleased(double x,double y,int button){
        if(draggingScroll && button==0){draggingScroll=false;return true;}return super.mouseReleased(x,y,button);
    }
    @Override public boolean keyPressed(int key,int scan,int mods){
        if(key==GLFW.GLFW_KEY_LEFT || key==GLFW.GLFW_KEY_UP){selected=Race.values()[(selected.ordinal()+4)%5];updatePreview();return true;}
        if(key==GLFW.GLFW_KEY_RIGHT || key==GLFW.GLFW_KEY_DOWN){selected=Race.values()[(selected.ordinal()+1)%5];updatePreview();return true;}
        if(key==GLFW.GLFW_KEY_TAB){tab=1-tab;scroll=0;return true;}
        if(key==GLFW.GLFW_KEY_PAGE_DOWN || key==GLFW.GLFW_KEY_PAGE_UP){moveScroll(key==GLFW.GLFW_KEY_PAGE_DOWN?100:-100);return true;}
        if(key==GLFW.GLFW_KEY_HOME){scroll=0;return true;}
        if(key==GLFW.GLFW_KEY_END){scroll=maxScroll();return true;}
        if(key==GLFW.GLFW_KEY_ENTER)return mouseClicked((buttonX+2)*uiScale,(buttonY+2)*uiScale,0);
        return super.keyPressed(key,scan,mods);
    }
    @Override public boolean isPauseScreen(){return false;}
}
