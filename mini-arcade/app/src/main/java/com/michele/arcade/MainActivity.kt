package com.michele.arcade

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*
import kotlin.random.Random
import kotlinx.coroutines.delay

private data class Game(val id:Int,val name:String,val emoji:String,val desc:String)
private val games = listOf(
 Game(1,"Tap Rush","⚡","10 hits. No mercy."), Game(2,"Reaction","🟢","Wait. Then strike."),
 Game(3,"Bullseye","🎯","Precision beats speed."), Game(4,"Memory Grid","🧠","Remember the flash."),
 Game(5,"Gravity","🌀","Survive the falling field."), Game(6,"Neon Snake","🐍","Eat. Turn. Survive."),
 Game(7,"Stack Perfect","🧱","Build the impossible."), Game(8,"Math Blitz","➗","Solve before zero."),
 Game(9,"Color Trap","🎨","Trust color, not words."), Game(10,"Simon X","🔴","Repeat the signal."),
 Game(11,"Pong Duel","🏓","Beat the machine."), Game(12,"Minefield","💣","One wrong tap."),
 Game(13,"Dodge","☄️","Move or disappear."), Game(14,"Perfect Timing","⏱️","Hit the moving zone."),
 Game(15,"Orbit","🪐","Catch the orbiting core."), Game(16,"Match Up","🃏","Find the pairs."),
 Game(17,"Maze","🧭","Escape in minimum moves."), Game(18,"Quick Tap","👆","Choose the right symbol."),
 Game(19,"Coin Storm","🪙","Grab gold, avoid skulls."), Game(20,"Chaos","👑","Everything at once.")
)

class MainActivity: ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { ArcadeApp() } }
}

@Composable
fun ArcadeApp(){
 MaterialTheme(colorScheme=darkColorScheme(background=Color(0xFF070912),surface=Color(0xFF111526),primary=Color(0xFF9B7BFF))){
  var selected by remember { mutableStateOf<Game?>(null) }; var best by remember { mutableIntStateOf(0) }
  if(selected==null) Home({selected=it},best) else GameScreen(selected!!,{selected=null},{s->best=max(best,s)})
 }
}

@Composable
fun Home(onPick:(Game)->Unit,best:Int){
 Column(Modifier.fillMaxSize().background(Color(0xFF070912)).padding(18.dp)){
  Text("20X",fontSize=48.sp,fontWeight=FontWeight.Black,color=Color.White)
  Text("MINI ARCADE",fontSize=18.sp,fontWeight=FontWeight.Bold,color=Color(0xFF9B7BFF))
  Row(verticalAlignment=Alignment.CenterVertically){
   Text("20 giochi · una sola sfida",color=Color.LightGray,modifier=Modifier.weight(1f))
   Surface(shape=RoundedCornerShape(50),color=Color(0xFF151A2B)){ Text("BEST " + best,modifier=Modifier.padding(horizontal=14.dp,vertical=8.dp),fontWeight=FontWeight.Bold) }
  }
  Spacer(Modifier.height(16.dp))
  LazyVerticalGrid(columns=GridCells.Fixed(2),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(10.dp),modifier=Modifier.weight(1f)){
   items(games){g->
    Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF111526)),shape=RoundedCornerShape(20.dp),modifier=Modifier.fillMaxWidth().height(132.dp).clickable{onPick(g)}){
     Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.SpaceBetween){
      Row(verticalAlignment=Alignment.CenterVertically){ Text(g.emoji,fontSize=28.sp,modifier=Modifier.weight(1f)); Text("#"+g.id,color=Color(0xFF555D78),fontWeight=FontWeight.Bold) }
      Text(g.name,fontWeight=FontWeight.Bold,fontSize=17.sp); Text(g.desc,color=Color(0xFF8C94AD),fontSize=12.sp)
     }
    }
   }
  }
  Text("Tocca un gioco. Batti il record. Sblocca la follia.",color=Color(0xFF6F7690),fontSize=12.sp,modifier=Modifier.align(Alignment.CenterHorizontally))
 }
}

@Composable
fun GameScreen(game:Game,onBack:()->Unit,onScore:(Int)->Unit){
 var score by remember { mutableIntStateOf(0) }; var round by remember { mutableIntStateOf(0) }; var lives by remember { mutableIntStateOf(3) }
 var over by remember { mutableStateOf(false) }; var message by remember { mutableStateOf("PRONTO?") }; var seed by remember { mutableIntStateOf(Random.nextInt()) }
 val target=remember(round,seed){Offset(Random.nextFloat()*.72f+.14f,Random.nextFloat()*.64f+.18f)}
 val selectedCell=remember(round,seed){Random.nextInt(9)}; var flash by remember { mutableStateOf(false) }; var timer by remember { mutableFloatStateOf(1f) }

 LaunchedEffect(game.id,round,seed){
  flash=false
  if(game.id==2){ message="ASPETTA..."; delay(900L+Random.nextLong(900)); flash=true; message="AGORA!" }
  if(game.id==4||game.id==10){flash=true;delay(650);flash=false}
  if(game.id==14){timer=0f;while(!over&&round<10){delay(24);timer=(timer+.018f)%1f}}
 }

 fun hit(x:Float,y:Float){
  if(over)return
  when(game.id){
   1,3,6,7,9,11,13,15,16,18,19,20->{val d=hypot(x-target.x,y-target.y);if(game.id==6||game.id==7||game.id==9||game.id==11||game.id==16||game.id==18){score+=100;round++;seed++}else if(d<.22f){score+=100;round++;seed++}else{lives--;if(lives<=0)over=true}}
   2->{if(flash){score+=150;round++;seed++}else{lives--;message="TROPPO PRESTO";if(lives<=0)over=true}}
   4,5,10,12,17->{val c=(x*3).toInt().coerceIn(0,2)+(y*3).toInt().coerceIn(0,2)*3;if(c==selectedCell){score+=120;round++;seed++}else{lives--;if(lives<=0)over=true}}
   8->{score+=100;round++;seed++}
   14->{if(timer>.40f&&timer<.60f){score+=250;round++;seed++}else{lives--;if(lives<=0)over=true}}
  }
  if(round>=10){over=true;onScore(score)}
 }

 Column(Modifier.fillMaxSize().background(Color(0xFF070912)).padding(16.dp)){
  Row(verticalAlignment=Alignment.CenterVertically){
   Text("‹",fontSize=40.sp,modifier=Modifier.clickable{onBack()})
   Column(Modifier.weight(1f).padding(start=10.dp)){Text(game.emoji+"  "+game.name,fontWeight=FontWeight.Black,fontSize=21.sp);Text(game.desc,color=Color(0xFF858CA4),fontSize=12.sp)}
   Text("SCORE "+score,fontWeight=FontWeight.Bold,color=palette[game.id%palette.size])
  }
  Spacer(Modifier.height(12.dp));LinearProgressIndicator(progress={min(1f,round/10f)},modifier=Modifier.fillMaxWidth(),color=palette[game.id%palette.size],trackColor=Color(0xFF1B2033))
  Spacer(Modifier.height(10.dp))
  Box(Modifier.fillMaxWidth().weight(1f).background(Color(0xFF0D1120),RoundedCornerShape(28.dp)).pointerInput(game.id,round,seed){detectTapGestures{p->hit(p.x/size.width,p.y/size.height)}}){
   Canvas(Modifier.fillMaxSize()){
    val w=size.width;val h=size.height
    when(game.id){
     4,5,10,12,17->{for(i in 0 until 9){val cx=(i%3+.5f)*w/3;val cy=(i/3+.5f)*h/3;val active=i==selectedCell&&(flash||game.id==5||game.id==12||game.id==17);drawRoundRect(if(active)palette[game.id%palette.size] else Color(0xFF171C30),Rect(cx-w/3+8,cy-h/3+8,cx+w/3-8,cy+h/3-8),18f)}}
     7->{for(i in 0..round)drawRoundRect(palette[(i+1)%palette.size],Rect(w*.25f-i*2,h*.82f-i*28,w*.75f+i*2,h*.82f-i*28+24),12f)}
     11->{drawCircle(Color.White,18f,Offset(w*.5f,h*(.25f+(round%5)*.12f)));drawRoundRect(palette[1],Rect(w*.35f,h*.82f,w*.65f,h*.86f),18f)}
     14->{drawRoundRect(Color(0xFF222943),Rect(w*.08f,h*.45f,w*.92f,h*.55f),20f);drawRoundRect(palette[3],Rect(w*.45f,h*.40f,w*.55f,h*.60f),14f);drawCircle(palette[1],20f,Offset(w*(.08f+.84f*timer),h*.5f))}
     else->{val r=if(game.id==3)52f else 68f;drawCircle(palette[game.id%palette.size],r,Offset(w*target.x,h*target.y));drawCircle(Color.White,r*.22f,Offset(w*target.x,h*target.y))}
    }
   }
   Column(Modifier.align(Alignment.Center),horizontalAlignment=Alignment.CenterHorizontally){
    when(game.id){
     8->{Text((round+2).toString()+" × "+(round+3),fontSize=48.sp,fontWeight=FontWeight.Black);Text("TAPPA PER RISOLVERE",color=Color(0xFF858CA4))}
     9->{Text(if(round%2==0)"ROSSO" else "BLU",fontSize=44.sp,fontWeight=FontWeight.Black,color=if(round%2==0)Color.Red else Color.Cyan);Text("NON LEGGERE. GUARDA.",color=Color(0xFF858CA4))}
     18->{Text(if(round%2==0)"◆" else "●",fontSize=80.sp,color=palette[(round+2)%palette.size]);Text("TAPPA IL SIMBOLO",color=Color.White)}
     16->{Text("PAIR",fontSize=56.sp,fontWeight=FontWeight.Black);Text("Trova la coppia",color=Color(0xFF858CA4))}
     6->{Text("🐍",fontSize=70.sp);Text("TAPPA PER CURVARE",color=Color.White)}
     20->{Text("CHAOS",fontSize=54.sp,fontWeight=FontWeight.Black,color=palette[round%palette.size]);Text("TAPPA. SOPRAVVIVI.",color=Color.White)}
     else->{Text(message,fontSize=28.sp,fontWeight=FontWeight.Black,color=Color.White)}
    }
   }
   if(over){
    Surface(color=Color(0xEE111526),shape=RoundedCornerShape(28.dp),modifier=Modifier.align(Alignment.Center).padding(28.dp)){
     Column(Modifier.padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally){
      Text("GAME OVER",fontSize=30.sp,fontWeight=FontWeight.Black);Text(score.toString()+" PUNTI",fontSize=22.sp,color=palette[game.id%palette.size]);Spacer(Modifier.height(12.dp))
      Button(onClick={round=0;score=0;lives=3;over=false;seed=Random.nextInt()}){Text("RIGIOCA")};TextButton(onClick=onBack){Text("ARCADE")}
     }
    }
   }
  }
  Spacer(Modifier.height(10.dp));Row(verticalAlignment=Alignment.CenterVertically){Text("ROUND "+min(round,10)+"/10",color=Color(0xFF858CA4),modifier=Modifier.weight(1f));Text("♥".repeat(lives.coerceAtLeast(0)),color=Color(0xFFFF3D81),fontSize=18.sp)}
 }
}
private val palette=listOf(Color(0xFF7C4DFF),Color(0xFF00D9FF),Color(0xFFFF3D81),Color(0xFFFFC107),Color(0xFF00E676),Color(0xFFFF7043))
private fun hypot(a:Float,b:Float)=sqrt(a*a+b*b)
