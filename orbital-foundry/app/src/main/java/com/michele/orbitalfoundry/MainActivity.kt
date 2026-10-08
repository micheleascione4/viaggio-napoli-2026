package com.michele.orbitalfoundry

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.foundation.border
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.*

private val Bg=Color(0xFF05060B)
private val Panel=Color(0xFF0E1220)
private val Panel2=Color(0xFF151A2A)
private val Ink=Color(0xFFF4F7FF)
private val Muted=Color(0xFF8D95AA)
private val Cyan=Color(0xFF45D7FF)
private val Violet=Color(0xFF9A7BFF)
private val Green=Color(0xFF45E0A8)
private val Orange=Color(0xFFFFB74D)
private val Red=Color(0xFFFF5874)

enum class Screen{HOME,BUILD,MISSIONS,FLIGHT,MAP}
enum class Guidance{MANUAL,PROGRADE,RETROGRADE,HOLD_ORBIT}

data class V2(val x:Double,val y:Double){
    operator fun plus(o:V2)=V2(x+o.x,y+o.y)
    operator fun minus(o:V2)=V2(x-o.x,y-o.y)
    operator fun times(k:Double)=V2(x*k,y*k)
    fun mag()=sqrt(x*x+y*y)
}

enum class PartType(val title:String,val emoji:String,val mass:Double,val fuel:Double,val thrust:Double,val color:Color){
    NOSE("Nose Cone","◢",1.5,0.0,0.0,Violet),
    CAPSULE("Crew Capsule","◉",3.5,0.0,0.0,Cyan),
    TANK("Fuel Tank","▣",4.0,16.0,0.0,Color(0xFF6C7BFF)),
    ENGINE("Vector Engine","▲",2.5,0.0,220.0,Orange),
    FIN("Control Fins","⌁",1.0,0.0,0.0,Green),
    DECOUPLER("Decoupler","⊙",0.8,0.0,0.0,Red),
    SOLAR("Solar Panel","✦",0.7,0.0,0.0,Color(0xFFFFD54F))
}

data class Rocket(val parts:List<PartType>){
    val dryMass get()=parts.sumOf{it.mass}
    val fuel get()=parts.sumOf{it.fuel}
    val thrust get()=parts.sumOf{it.thrust}
    val hasEngine get()=parts.any{it==PartType.ENGINE}
    val hasCapsule get()=parts.any{it==PartType.CAPSULE}
    val deltaV get():Double{
        val m0=(dryMass+fuel).coerceAtLeast(0.1)
        val m1=dryMass.coerceAtLeast(0.1)
        return if(hasEngine&&fuel>0)9.6*ln(m0/m1) else 0.0
    }
}

data class SimState(
    val pos:V2=V2(0.0,53.0),
    val vel:V2=V2(6.1,0.0),
    val angle:Double=PI/2,
    val throttle:Double=.95,
    val fuel:Double,
    val mass:Double,
    val thrust:Double,
    val time:Double=0.0,
    val stage:Int=1,
    val guidance:Guidance=Guidance.MANUAL,
    val crashed:Boolean=false,
    val landed:Boolean=false,
    val trail:List<V2> = emptyList()
)

class MainActivity:ComponentActivity(){
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        setContent{OrbitalFoundryApp()}
    }
}

@Composable
fun OrbitalFoundryApp(){
    MaterialTheme(colorScheme=darkColorScheme(background=Bg,surface=Panel,primary=Violet,onBackground=Ink,onSurface=Ink)){
        var screen by remember{mutableStateOf(Screen.HOME)}
        var rocket by remember{mutableStateOf(Rocket(listOf(PartType.NOSE,PartType.CAPSULE,PartType.TANK,PartType.ENGINE,PartType.FIN)))}
        var sim by remember{mutableStateOf<SimState?>(null)}
        var missions by remember{mutableStateOf(setOf<String>())}
        var career by remember{mutableStateOf(CareerState())}
        when(screen){
            Screen.HOME->HomeScreen(rocket,missions,career,{screen=Screen.BUILD},{if(rocket.hasEngine&&rocket.hasCapsule){sim=launchState(rocket);screen=Screen.FLIGHT}},{screen=Screen.MISSIONS})
            Screen.BUILD->BuilderScreen(rocket,{rocket=rocket.copy(parts=rocket.parts+it)},{p->val l=rocket.parts.toMutableList();val i=l.indexOfLast{it==p};if(i>=0)l.removeAt(i);rocket=rocket.copy(parts=l)},{rocket=Rocket(emptyList())},{screen=Screen.HOME},{if(rocket.hasEngine&&rocket.hasCapsule){sim=launchState(rocket);screen=Screen.FLIGHT}})
            Screen.MISSIONS->MissionScreen(missions,career,{career=it},{screen=Screen.HOME})
            Screen.FLIGHT->{val s=sim;if(s!=null)FlightScreen(s,{next->sim=next;missions=missions+missionCompletions(next)},{screen=Screen.MAP},{screen=Screen.HOME}){name->missions=missions+name}}
            Screen.MAP->{val s=sim;if(s!=null)MapScreen(s,{screen=Screen.FLIGHT})}
        }
    }
}

@Composable
private fun Shell(title:String,subtitle:String,onBack:()->Unit,content:@Composable ColumnScope.()->Unit){
    Column(Modifier.fillMaxSize().background(Bg).padding(16.dp)){
        Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.fillMaxWidth()){
            Text("‹",fontSize=42.sp,modifier=Modifier.clickable{onBack()}.padding(end=8.dp))
            Column(Modifier.weight(1f)){Text(title,fontSize=24.sp,fontWeight=FontWeight.Black);Text(subtitle,color=Muted,fontSize=12.sp)}
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun HomeScreen(rocket:Rocket,missions:Set<String>,career:CareerState,onBuild:()->Unit,onLaunch:()->Unit,onMissions:()->Unit){
    Column(Modifier.fillMaxSize().background(Bg).padding(18.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){
            Column(Modifier.weight(1f)){
                Text("ORBITAL",fontSize=42.sp,fontWeight=FontWeight.Black)
                Text("FOUNDRY",fontSize=18.sp,fontWeight=FontWeight.Bold,color=Violet)
                Text("Build. Fly. Orbit. Return.",color=Muted)
            }
            Box(Modifier.size(54.dp).background(Panel2,CircleShape),contentAlignment=Alignment.Center){Text("◉",fontSize=28.sp,color=Cyan)}
        }
        Spacer(Modifier.height(18.dp))
        Surface(color=Panel,shape=RoundedCornerShape(24.dp),modifier=Modifier.fillMaxWidth()){
            Column(Modifier.padding(18.dp)){
                Text("FLIGHT READY",color=Green,fontSize=12.sp,fontWeight=FontWeight.Black)
                Text("Current vehicle",fontSize=22.sp,fontWeight=FontWeight.Black)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.padding(top=6.dp)){
                    Metric("FUNDS","€${career.funds/1000}K",Modifier.weight(1f))
                    Metric("SCIENCE","${career.science}",Modifier.weight(1f))
                    Metric("TECH","${career.unlocked.size}",Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth()){
                    Metric("MASS","%.1f t".format(rocket.dryMass+rocket.fuel),Modifier.weight(1f))
                    Metric("FUEL","%.1f t".format(rocket.fuel),Modifier.weight(1f))
                    Metric("ΔV","%.0f m/s".format(rocket.deltaV),Modifier.weight(1f))
                }
                Spacer(Modifier.height(14.dp))
                Button(onClick=onLaunch,enabled=rocket.hasEngine&&rocket.hasCapsule,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp)){Text("LAUNCH MISSION",fontWeight=FontWeight.Black)}
                OutlinedButton(onClick=onBuild,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp)){Text("OPEN VEHICLE LAB")}
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp),modifier=Modifier.fillMaxWidth()){
            ActionCard("🛰","VEHICLE LAB","Build custom stages",onBuild,Modifier.weight(1f))
            ActionCard("◆","MISSIONS",missions.size.toString()+" completed",onMissions,Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        Surface(color=Panel,shape=RoundedCornerShape(20.dp),modifier=Modifier.fillMaxWidth()){
            Column(Modifier.padding(16.dp)){
                Text("DESIGN GOAL",fontSize=12.sp,color=Muted,fontWeight=FontWeight.Black)
                Text("FLIGHT HARDWARE · VISUAL PREVIEW",fontSize=18.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=4.dp))
                Text("Metallic parts, panel lines, mission telemetry, atmospheric glow and engine exhaust. Next: procedural textures, real staging and 3D surface terrain.",color=Muted,fontSize=13.sp,modifier=Modifier.padding(top=4.dp))
            }
        }
    }
}

@Composable
private fun Metric(label:String,value:String,modifier:Modifier=Modifier){
    Surface(color=Panel2,shape=RoundedCornerShape(14.dp),modifier=modifier){
        Column(Modifier.padding(10.dp),horizontalAlignment=Alignment.CenterHorizontally){
            Text(label,color=Muted,fontSize=10.sp,fontWeight=FontWeight.Bold)
            Text(value,fontSize=15.sp,fontWeight=FontWeight.Black)
        }
    }
}

@Composable
private fun ActionCard(icon:String,title:String,sub:String,onClick:()->Unit,modifier:Modifier){
    Surface(color=Panel,shape=RoundedCornerShape(20.dp),modifier=modifier.clickable{onClick()}){
        Column(Modifier.padding(16.dp)){
            Text(icon,fontSize=24.sp,color=Cyan)
            Text(title,fontWeight=FontWeight.Black,modifier=Modifier.padding(top=8.dp))
            Text(sub,fontSize=11.sp,color=Muted)
        }
    }
}

@Composable
private fun RocketPartThumbnail(part:PartType){
    Canvas(Modifier.size(width=38.dp,height=42.dp)){
        drawRoundRect(Color(0xFF0A0F1A),Offset(0f,0f),Size(size.width,size.height),CornerRadius(5f))
        drawRocketComponent(part,size.width*.18f,size.height*.08f,size.width*.64f,size.height*.82f)
    }
}

@Composable
private fun RocketStackPreview(rocket:Rocket){
    Canvas(Modifier.fillMaxSize()){
        drawRect(Brush.verticalGradient(listOf(Color(0xFF10192B),Color(0xFF070A13))),size=Size(size.width,size.height))
        for(x in 0..(size.width/28f).toInt()) drawLine(Color(0x263C5579),Offset(x*28f,0f),Offset(x*28f,size.height),1f)
        for(y in 0..(size.height/28f).toInt()) drawLine(Color(0x263C5579),Offset(0f,y*28f),Offset(size.width,y*28f),1f)
        val halo=Offset(size.width*.5f,size.height*.56f)
        drawCircle(Brush.radialGradient(listOf(Color(0x443E8BFF),Color(0x113E8BFF),Color.Transparent),halo,size.minDimension*.58f),size.minDimension*.58f,halo)
        val count=rocket.parts.size.coerceAtLeast(1)
        val partH=min(38f,(size.height-104f)/count).coerceAtLeast(18f)
        val partGap=2f
        val partW=size.width*.36f
        val left=(size.width-partW)*.5f
        val total=count*(partH+partGap)
        var y=size.height-62f-partH
        rocket.parts.asReversed().forEach{part->
            drawRocketComponent(part,left,y,partW,partH)
            y-=partH+partGap
        }
        // Mobile launch clamp and illuminated platform.
        drawRoundRect(Color(0xFF202D43),Offset(size.width*.25f,size.height-54f),Size(size.width*.5f,10f),CornerRadius(4f))
        drawLine(Color(0xFF45D7FF),Offset(size.width*.18f,size.height-43f),Offset(size.width*.82f,size.height-43f),2f)
        drawCircle(Color(0x6645D7FF),size.width*.32f,Offset(size.width*.5f,size.height-42f),style=Stroke(width=2f))
        if(rocket.parts.isEmpty()){
            drawCircle(Color(0x8845D7FF),18f,Offset(size.width*.5f,size.height*.42f),style=Stroke(width=2f))
        }
    }
}

private fun DrawScope.drawRocketComponent(part:PartType,left:Float,top:Float,width:Float,height:Float){
    val cx=left+width/2f
    val edge=Color(0xFF111827)
    val metal=Brush.horizontalGradient(listOf(Color(0xFF192231),part.color,Color(0xFFE6EDF7),part.color,Color(0xFF202A3A)),left,left+width)
    // Contact shadow provides separation between stacked modules.
    drawRoundRect(Color(0x77000000),Offset(left+3f,top+3f),Size(width,height),CornerRadius(4f))
    when(part){
        PartType.NOSE->{
            val p=Path().apply{moveTo(cx,top);lineTo(left+width*.91f,top+height*.78f);quadraticTo(cx,top+height*1.06f,left+width*.09f,top+height*.78f);close()}
            drawPath(p,Brush.horizontalGradient(listOf(Color(0xFF384254),Color(0xFFF4F7FC),part.color,Color(0xFF252E3E)),left,left+width))
            drawPath(p,edge,style=Stroke(1.6f))
            drawLine(Color(0xFFFFD28A),Offset(cx,top+height*.17f),Offset(cx,top+height*.70f),1.5f)
            drawRoundRect(Color(0xFF111B2A),Offset(cx-width*.18f,top+height*.50f),Size(width*.36f,height*.12f),CornerRadius(3f))
        }
        PartType.CAPSULE->{
            val p=Path().apply{moveTo(cx,top);cubicTo(left+width*.86f,top+height*.05f,left+width*.94f,top+height*.24f,left+width*.88f,top+height*.50f);lineTo(left+width*.84f,top+height*.92f);lineTo(left+width*.16f,top+height*.92f);lineTo(left+width*.12f,top+height*.50f);cubicTo(left+width*.06f,top+height*.24f,left+width*.14f,top+height*.05f,cx,top);close()}
            drawPath(p,metal);drawPath(p,edge,style=Stroke(1.6f))
            drawRoundRect(Color(0xFF182638),Offset(cx-width*.22f,top+height*.30f),Size(width*.44f,height*.25f),CornerRadius(height*.12f))
            drawCircle(Color(0xFF69D9FF),height*.08f,Offset(cx,top+height*.42f))
            drawRoundRect(Color(0xFFB8C6D8),Offset(left+width*.10f,top+height*.82f),Size(width*.80f,height*.10f),CornerRadius(2f))
        }
        PartType.TANK->{
            drawRoundRect(metal,Offset(left+width*.07f,top+height*.02f),Size(width*.86f,height*.96f),CornerRadius(width*.11f))
            drawRoundRect(edge,Offset(left+width*.07f,top+height*.02f),Size(width*.86f,height*.96f),CornerRadius(width*.11f),style=Stroke(1.4f))
            for(i in 0..3){
                val y=top+height*(.10f+i*.25f)
                drawLine(Color(0xFF1B2739),Offset(left+width*.11f,y),Offset(left+width*.89f,y),2f)
                drawLine(Color(0x99FFFFFF),Offset(left+width*.16f,y+2f),Offset(left+width*.84f,y+2f),.9f)
            }
            drawRoundRect(Color(0xFFD8E4EF),Offset(left+width*.41f,top+height*.19f),Size(width*.18f,height*.46f),CornerRadius(2f))
            drawLine(Color(0xFFFFC46E),Offset(left+width*.20f,top+height*.69f),Offset(left+width*.80f,top+height*.69f),2f)
        }
        PartType.ENGINE->{
            drawRoundRect(metal,Offset(left+width*.20f,top),Size(width*.60f,height*.34f),CornerRadius(2f))
            val nozzle=Path().apply{moveTo(left+width*.27f,top+height*.28f);lineTo(left+width*.73f,top+height*.28f);lineTo(left+width*.92f,top+height*.86f);quadraticTo(cx,top+height*1.08f,left+width*.08f,top+height*.86f);close()}
            drawPath(nozzle,Brush.horizontalGradient(listOf(Color(0xFF171E2B),Color(0xFFBAC7D8),Color(0xFF354154),Color(0xFF0D1421)),left,left+width))
            drawPath(nozzle,edge,style=Stroke(1.5f))
            drawOval(Color(0xFF05070C),Offset(left+width*.22f,top+height*.72f),Size(width*.56f,height*.18f))
            drawOval(Color(0xFFFFA53F),Offset(left+width*.35f,top+height*.76f),Size(width*.30f,height*.10f))
            drawLine(Color(0xFFFFD27F),Offset(cx,top+height*.79f),Offset(cx,top+height*.90f),2f)
        }
        PartType.FIN->{
            val leftFin=Path().apply{moveTo(left+width*.18f,top+height*.25f);lineTo(left-width*.03f,top+height*.90f);lineTo(left+width*.32f,top+height*.79f);lineTo(left+width*.38f,top+height*.25f);close()}
            val rightFin=Path().apply{moveTo(left+width*.82f,top+height*.25f);lineTo(left+width*1.03f,top+height*.90f);lineTo(left+width*.68f,top+height*.79f);lineTo(left+width*.62f,top+height*.25f);close()}
            drawPath(leftFin,part.color);drawPath(rightFin,part.color)
            drawRoundRect(metal,Offset(left+width*.28f,top),Size(width*.44f,height),CornerRadius(3f))
            drawLine(Color.White.copy(alpha=.65f),Offset(cx,top+3f),Offset(cx,top+height-3f),1.1f)
        }
        PartType.DECOUPLER->{
            drawRoundRect(metal,Offset(left+width*.04f,top+height*.17f),Size(width*.92f,height*.66f),CornerRadius(4f))
            drawRoundRect(Color(0xFFD3DEED),Offset(left+width*.02f,top+height*.15f),Size(width*.96f,height*.20f),CornerRadius(2f))
            drawRoundRect(Color(0xFF26354A),Offset(left+width*.02f,top+height*.66f),Size(width*.96f,height*.20f),CornerRadius(2f))
            for(i in 0..5) drawCircle(Color(0xFFFFB74D),1.4f,Offset(left+width*(.16f+i*.136f),top+height*.49f))
        }
        PartType.SOLAR->{
            drawRoundRect(Color(0xFFB6C5D9),Offset(cx-width*.10f,top+height*.35f),Size(width*.20f,height*.30f),CornerRadius(2f))
            drawRect(Brush.verticalGradient(listOf(Color(0xFF144A91),Color(0xFF071C46))),Offset(left,top+height*.10f),Size(width*.36f,height*.80f))
            drawRect(Brush.verticalGradient(listOf(Color(0xFF144A91),Color(0xFF071C46))),Offset(left+width*.64f,top+height*.10f),Size(width*.36f,height*.80f))
            for(i in 1..3){
                drawLine(Color(0xFF4EC8FF),Offset(left+width*.36f*i/3f,top+height*.1f),Offset(left+width*.36f*i/3f,top+height*.9f),.8f)
                drawLine(Color(0xFF4EC8FF),Offset(left+width*.64f+width*.36f*i/3f,top+height*.1f),Offset(left+width*.64f+width*.36f*i/3f,top+height*.9f),.8f)
            }
            drawRect(Color(0xFF66D5FF),Offset(left,top+height*.10f),Size(width*.36f,height*.80f),style=Stroke(1.1f))
            drawRect(Color(0xFF66D5FF),Offset(left+width*.64f,top+height*.10f),Size(width*.36f,height*.80f),style=Stroke(1.1f))
        }
    }
}

@Composable
private fun BuilderScreen(rocket:Rocket,onAdd:(PartType)->Unit,onRemove:(PartType)->Unit,onClear:()->Unit,onBack:()->Unit,onLaunch:()->Unit){
    Shell("Vehicle Lab","Tap parts to add · stack them into a launcher",onBack){
        Row(verticalAlignment=Alignment.Top,modifier=Modifier.fillMaxWidth()){
            Surface(color=Panel,shape=RoundedCornerShape(20.dp),modifier=Modifier.weight(1f).height(420.dp)){
                Box(Modifier.fillMaxSize(),contentAlignment=Alignment.BottomCenter){
                    RocketStackPreview(rocket)
                    Text(if(rocket.parts.isEmpty())"ADD A CORE" else "STACK PREVIEW",color=Muted,fontSize=11.sp,modifier=Modifier.padding(bottom=10.dp))
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.width(145.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                PartType.entries.forEach{p->
                    Surface(color=Panel2,shape=RoundedCornerShape(14.dp),modifier=Modifier.fillMaxWidth().clickable{onAdd(p)}){
                        Row(Modifier.padding(6.dp),verticalAlignment=Alignment.CenterVertically){
                            RocketPartThumbnail(p)
                            Spacer(Modifier.width(5.dp))
                            Column(Modifier.weight(1f)){
                                Text(p.title,fontWeight=FontWeight.Bold,fontSize=11.sp,lineHeight=12.sp)
                                Text("M %.1f · F %.0f".format(p.mass,p.fuel),fontSize=8.sp,color=Muted)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth()){
            Text("PARTS "+rocket.parts.size,color=Muted,fontSize=12.sp,modifier=Modifier.weight(1f).align(Alignment.CenterVertically))
            OutlinedButton(onClick=onClear){Text("RESET")}
            Button(onClick=onLaunch,enabled=rocket.hasEngine&&rocket.hasCapsule){Text("LAUNCH")}
        }
        Spacer(Modifier.height(8.dp))
        Text("STACK",color=Muted,fontSize=11.sp,fontWeight=FontWeight.Black)
        LazyColumn(verticalArrangement=Arrangement.spacedBy(6.dp),modifier=Modifier.weight(1f)){
            items(rocket.parts.asReversed()){p->
                Surface(color=Panel,shape=RoundedCornerShape(12.dp),modifier=Modifier.fillMaxWidth()){
                    Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.padding(horizontal=12.dp,vertical=8.dp)){
                        RocketPartThumbnail(p)
                        Column(Modifier.weight(1f).padding(start=8.dp)){Text(p.title,fontWeight=FontWeight.Bold,fontSize=13.sp);Text("mass %.1f · fuel %.1f · thrust %.0f".format(p.mass,p.fuel,p.thrust),color=Muted,fontSize=9.sp)}
                        Text("−",fontSize=22.sp,color=Red,modifier=Modifier.clickable{onRemove(p)})
                    }
                }
            }
        }
    }
}

@Composable
private fun MissionScreen(missions:Set<String>,career:CareerState,onCareer:(CareerState)->Unit,onBack:()->Unit){
    Shell("Mission Control","Contracts · rewards · technology progression",onBack){
        LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){
            items(CareerDatabase.contracts){contract->
                val done=contract.id in career.completedContracts
                val available=canAccept(contract,career) && contract.id in missions
                Surface(color=Panel,shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth()){
                    Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.padding(14.dp)){
                        Column(Modifier.weight(1f)){
                            Row(verticalAlignment=Alignment.CenterVertically){
                                Text(if(done)"✓" else "○",fontSize=24.sp,color=if(done)Green else Cyan)
                                Text(contract.title,fontWeight=FontWeight.Black,fontSize=16.sp,modifier=Modifier.padding(start=10.dp))
                            }
                            Text(contract.description,color=Muted,fontSize=11.sp,modifier=Modifier.padding(start=34.dp,top=3.dp))
                            Text("€${contract.reward/1000}K  ·  ${contract.science} SCI",color=Orange,fontSize=10.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(start=34.dp,top=5.dp))
                        }
                        if(done) Text("DONE",color=Green,fontSize=9.sp,fontWeight=FontWeight.Black)
                        else Button(onClick={onCareer(completeContract(contract,career))},enabled=available){Text(if(available)"CLAIM" else if(contract.id in missions)"READY" else "LOCK",fontSize=9.sp)}
                    }
                }
            }
            item{
                Spacer(Modifier.height(4.dp))
                Text("TECH TREE",fontWeight=FontWeight.Black,fontSize=12.sp,color=Muted)
            }
            items(CareerDatabase.tech){tech->
                val unlocked=tech.id in career.unlocked
                val upgrade=unlockTech(tech,career)
                Surface(color=if(unlocked)Panel2 else Panel,shape=RoundedCornerShape(16.dp),modifier=Modifier.fillMaxWidth()){
                    Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.padding(14.dp)){
                        Text(if(unlocked)"◆" else "◇",color=if(unlocked)Green else Violet,fontSize=22.sp)
                        Column(Modifier.weight(1f).padding(start=10.dp)){
                            Text(tech.title,fontWeight=FontWeight.Bold)
                            Text(if(unlocked)"UNLOCKED" else "€${tech.cost/1000}K · ${tech.science} SCI",color=Muted,fontSize=10.sp)
                        }
                        if(!unlocked) Button(onClick={upgrade?.let(onCareer)},enabled=upgrade!=null){Text("UNLOCK",fontSize=9.sp)}
                    }
                }
            }
        }
    }
}

private fun missionCompletions(s:SimState):Set<String>{
    val alt=s.pos.mag()-50.0
    val orbit=orbitalMetrics(s.pos,s.vel)
    val done=mutableSetOf<String>()
    if(alt>=80) done+="suborbital"
    if(alt>=80 && orbit.periapsis-50.0>=50 && orbit.apoapsis-50.0>=80) done+="orbit"
    if(alt>=120 && orbit.periapsis-50.0>=120) done+="satellite"
    if(s.landed) done+="recovery"
    if(s.landed) done+="suborbital"
    return done
}

private fun launchState(rocket:Rocket)=SimState(fuel=rocket.fuel,mass=rocket.dryMass+rocket.fuel,thrust=rocket.thrust)

@Composable
private fun FlightScreen(sim:SimState,onTick:(SimState)->Unit,onMap:()->Unit,onBack:()->Unit,onMission:(String)->Unit){
    var paused by remember{mutableStateOf(false)}
    var pitchOffset by remember{mutableFloatStateOf(90f)}
    var timeWarp by remember{mutableIntStateOf(1)}
    LaunchedEffect(paused,timeWarp,sim.throttle,sim.guidance,sim.crashed,sim.landed){
        if(paused||sim.crashed||sim.landed)return@LaunchedEffect
        var local=sim
        while(true){
            delay(32L)
            local=step(local,0.055*timeWarp,pitchOffset)
            onTick(local)
            if(local.crashed||local.landed)break
        }
    }
    val alt=sim.pos.mag()-50.0
    val speed=sim.vel.mag()
    val orbit=orbitalMetrics(sim.pos,sim.vel)
    val mission=when{
        sim.landed->"RECOVERY"
        orbit.periapsis-50>=120&&alt>=120->"SATELLITE ORBIT"
        orbit.periapsis-50>=50&&alt>=80->"STABLE ORBIT"
        alt>=80->"SUBORBITAL"
        alt>35->"ASCENT"
        else->"LAUNCH"
    }
    Column(Modifier.fillMaxSize().background(Bg).padding(12.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){
            Text("‹",fontSize=38.sp,modifier=Modifier.clickable{onBack()})
            Column(Modifier.weight(1f).padding(start=8.dp)){Text("MISSION · "+mission,fontWeight=FontWeight.Black,fontSize=17.sp);Text("STAGE "+sim.stage+"  ·  "+sim.guidance.name.replace('_',' '),color=Muted,fontSize=10.sp)}
            IconButton(onClick=onMap){Text("◎",fontSize=26.sp,color=Cyan)}
        }
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp),modifier=Modifier.fillMaxWidth()){
            Metric("ALT","%.0f km".format(alt),Modifier.weight(1f))
            Metric("VEL","%.1f km/s".format(speed),Modifier.weight(1f))
            Metric("APO","%.0f km".format(orbit.apoapsis-50),Modifier.weight(1f))
            Metric("PERI","%.0f km".format(orbit.periapsis-50),Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Surface(color=Panel,shape=RoundedCornerShape(22.dp),modifier=Modifier.fillMaxWidth().weight(1f)){
            Canvas(Modifier.fillMaxSize()){
                val scale=(min(size.width,size.height)/170f).coerceAtLeast(.5f)
                val center=Offset(size.width/2,size.height/2)
                // Deep-space backdrop and layered starfield.
                drawRect(Brush.verticalGradient(listOf(Color(0xFF070B18),Color(0xFF0D1630),Color(0xFF05060B))),size=Size(size.width,size.height))
                repeat(74){i->
                    val sx=((i*83+17)%997)/997f*size.width
                    val sy=((i*47+29)%991)/991f*size.height
                    val radius=if(i%9==0)2f else if(i%3==0)1.4f else .8f
                    drawCircle(if(i%8==0)Color(0xFF8FDFFF) else Color.White,radius,Offset(sx,sy),alpha=if(i%5==0).75f else .34f)
                }
                // Atmospheric halo, then a shaded Earth with land and cloud bands.
                val earthR=50*scale
                drawCircle(Brush.radialGradient(listOf(Color(0x5549BFFF),Color(0x2249BFFF),Color.Transparent),center,earthR*1.48f),earthR*1.48f,center)
                drawCircle(Brush.radialGradient(listOf(Color(0xFF3988C8),Color(0xFF1D4B83),Color(0xFF081326)),Offset(center.x-earthR*.32f,center.y-earthR*.38f),earthR*1.75f),earthR,center)
                // Stylized landmasses kept within the visible disc.
                val land1=Path().apply{
                    moveTo(center.x-earthR*.72f,center.y-earthR*.22f)
                    lineTo(center.x-earthR*.48f,center.y-earthR*.43f)
                    lineTo(center.x-earthR*.22f,center.y-earthR*.35f)
                    lineTo(center.x-earthR*.08f,center.y-earthR*.08f)
                    lineTo(center.x-earthR*.28f,center.y+earthR*.12f)
                    lineTo(center.x-earthR*.40f,center.y+earthR*.47f)
                    lineTo(center.x-earthR*.62f,center.y+earthR*.35f)
                    close()
                }
                drawPath(land1,Brush.linearGradient(listOf(Color(0xFF76B77B),Color(0xFF34755B)),Offset(center.x-earthR,center.y-earthR),Offset(center.x,center.y+earthR)))
                val land2=Path().apply{
                    moveTo(center.x+earthR*.10f,center.y-earthR*.45f)
                    lineTo(center.x+earthR*.48f,center.y-earthR*.32f)
                    lineTo(center.x+earthR*.66f,center.y-earthR*.05f)
                    lineTo(center.x+earthR*.42f,center.y+earthR*.19f)
                    lineTo(center.x+earthR*.25f,center.y+earthR*.42f)
                    lineTo(center.x+earthR*.08f,center.y+earthR*.13f)
                    lineTo(center.x-earthR*.02f,center.y-earthR*.12f)
                    close()
                }
                drawPath(land2,Brush.linearGradient(listOf(Color(0xFF83BB84),Color(0xFF3C8062)),Offset(center.x,center.y-earthR),Offset(center.x+earthR,center.y+earthR)))
                repeat(5){i->
                    val y=center.y-earthR*.56f+i*earthR*.24f
                    drawOval(Color(0x88D8F0FF),topLeft=Offset(center.x-earthR*.72f,y),size=Size(earthR*1.38f,earthR*.075f),style=Stroke(width=earthR*.04f))
                }
                drawCircle(Color(0x994BC3FF),earthR,center,style=Stroke(width=max(1.5f,earthR*.035f)))
                // Predicted trajectory.
                if(sim.trail.size>1){
                    val path=Path()
                    sim.trail.forEachIndexed{idx,p->
                        val q=worldToScreen(p,sim.pos,center,scale)
                        if(idx==0)path.moveTo(q.x,q.y) else path.lineTo(q.x,q.y)
                    }
                    drawPath(path,color=Color(0xAA45D7FF),style=Stroke(width=5f))
                    drawPath(path,color=Cyan,style=Stroke(width=2f),alpha=.9f)
                }
                // Moon marker and orbit guide.
                val moon=moonPos(sim.time)
                val ms=worldToScreen(moon,sim.pos,center,scale)
                drawCircle(Color(0x332A3142),19f,ms)
                drawCircle(Brush.radialGradient(listOf(Color(0xFFD6D8E0),Color(0xFF777F91),Color(0xFF3A4152)),Offset(ms.x-4f,ms.y-4f),24f),13f,ms)
                repeat(6){i->drawCircle(Color(0x55777F91),1.2f,Offset(ms.x+cos(i*1.1).toFloat()*7f,ms.y+sin(i*1.1).toFloat()*6f))}
                // Rocket silhouette follows the commanded attitude.
                val rp=worldToScreen(sim.pos,sim.pos,center,scale)
                val fx=cos(sim.angle).toFloat()
                val fy=-sin(sim.angle).toFloat()
                val bx=-fx
                val by=-fy
                val sideX=-fy
                val sideY=fx
                if(sim.fuel>0.01 && sim.throttle>0.01 && !sim.crashed && !sim.landed){
                    val flameLen=12f+sim.throttle.toFloat()*24f
                    val flame=Path().apply{
                        moveTo(rp.x+bx*8f+sideX*3.5f,rp.y+by*8f+sideY*3.5f)
                        lineTo(rp.x+bx*flameLen,rp.y+by*flameLen)
                        lineTo(rp.x+bx*8f-sideX*3.5f,rp.y+by*8f-sideY*3.5f)
                        close()
                    }
                    drawPath(flame,Brush.verticalGradient(listOf(Color(0xFFFFF4BC),Color(0xFFFFA43A),Color(0x55FF4D2E)),startY=rp.y-30f,endY=rp.y+30f))
                    drawCircle(Color(0x55FF8B38),9f,Offset(rp.x+bx*10f,rp.y+by*10f))
                }
                val rocketBody=Path().apply{
                    moveTo(rp.x+fx*13f,rp.y+fy*13f)
                    lineTo(rp.x-fx*8f+sideX*5f,rp.y-fy*8f+sideY*5f)
                    lineTo(rp.x-fx*6f,rp.y-fy*6f)
                    lineTo(rp.x-fx*8f-sideX*5f,rp.y-fy*8f-sideY*5f)
                    close()
                }
                drawPath(rocketBody,Brush.linearGradient(listOf(Color.White,Color(0xFF9BAAC1),Color(0xFF404D64)),Offset(rp.x-8f,rp.y-8f),Offset(rp.x+8f,rp.y+8f)))
                drawCircle(Color(0xFF45D7FF),2.2f,Offset(rp.x+fx*2.5f,rp.y+fy*2.5f))
                drawCircle(Color.White,2f,rp)
            }
        }
        Text("THROTTLE "+(sim.throttle*100).roundToInt()+"%",color=Muted,fontSize=10.sp,fontWeight=FontWeight.Bold)
        Slider(value=sim.throttle.toFloat(),onValueChange={onTick(sim.copy(throttle=it.toDouble()))},valueRange=0f..1f,colors=SliderDefaults.colors(thumbColor=Cyan,activeTrackColor=Cyan))
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp),modifier=Modifier.fillMaxWidth()){
            SmallButton(if(paused)"RESUME" else "PAUSE",{paused=!paused},Modifier.weight(1f))
            SmallButton("STAGE",{onTick(sim.copy(stage=(sim.stage+1).coerceAtMost(2)))},Modifier.weight(1f))
            SmallButton("P−",{pitchOffset=(pitchOffset-5f).coerceIn(0f,180f);onTick(sim.copy(guidance=Guidance.MANUAL))},Modifier.weight(1f))
            SmallButton("MAN",{onTick(sim.copy(guidance=Guidance.MANUAL))},Modifier.weight(1f))
            SmallButton("P+",{pitchOffset=(pitchOffset+5f).coerceIn(0f,180f);onTick(sim.copy(guidance=Guidance.MANUAL))},Modifier.weight(1f))
            SmallButton("PRO",{onTick(sim.copy(guidance=Guidance.PROGRADE))},Modifier.weight(1f))
            SmallButton("RET",{onTick(sim.copy(guidance=Guidance.RETROGRADE))},Modifier.weight(1f))
            SmallButton("WARP x"+timeWarp,{timeWarp=if(timeWarp==1)5 else if(timeWarp==5)20 else 1},Modifier.weight(1f))
        }
        if(sim.crashed){
            Surface(color=Color(0xEE1A0A11),shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth().padding(top=8.dp)){Text("VEHICLE LOST · RECOVERY REQUIRED",color=Red,fontWeight=FontWeight.Black,modifier=Modifier.padding(14.dp))}
        }else if(sim.landed){
            Surface(color=Color(0xEE0D2A1F),shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth().padding(top=8.dp)){Text("SOFT LANDING · OBJECTIVE ACHIEVED · CLAIM IN MISSION CONTROL",color=Green,fontWeight=FontWeight.Black,modifier=Modifier.padding(14.dp))}
        }
    }
}

@Composable
private fun SmallButton(text:String,onClick:()->Unit,modifier:Modifier=Modifier){
    Button(onClick=onClick,modifier=modifier.height(44.dp),shape=RoundedCornerShape(12.dp),contentPadding=PaddingValues(horizontal=4.dp),colors=ButtonDefaults.buttonColors(containerColor=Panel2)){Text(text,fontSize=9.sp,fontWeight=FontWeight.Black)}
}

@Composable
private fun MapScreen(sim:SimState,onBack:()->Unit){
    Shell("Navigation Map","System view · trajectory preview",onBack){
        Surface(color=Panel,shape=RoundedCornerShape(22.dp),modifier=Modifier.fillMaxWidth().weight(1f)){
            Canvas(Modifier.fillMaxSize().padding(10.dp)){
                val c=Offset(size.width/2,size.height/2)
                drawCircle(Color(0xFFFFD86E),26f,c)
                listOf(75f,135f,205f).forEach{r->drawCircle(Color(0xFF263148),r,c,style=Stroke(width=1.5f))}
                drawCircle(Color(0xFF2E63A1),12f,Offset(c.x+75f,c.y))
                drawCircle(Color(0xFFAAAAAF),5f,Offset(c.x+135f,c.y))
                drawCircle(Color(0xFFAA4A35),10f,Offset(c.x+205f,c.y))
                val path=Path()
                sim.trail.takeLast(240).forEachIndexed{idx,p->
                    val q=Offset(c.x+(p.x/400.0).toFloat()*min(size.width,size.height),c.y-(p.y/400.0).toFloat()*min(size.width,size.height))
                    if(idx==0)path.moveTo(q.x,q.y) else path.lineTo(q.x,q.y)
                }
                if(sim.trail.isNotEmpty())drawPath(path,color=Cyan,style=Stroke(width=3f))
            }
        }
        Spacer(Modifier.height(8.dp))
        Surface(color=Panel,shape=RoundedCornerShape(18.dp)){
            Column(Modifier.padding(14.dp)){
                Text("NAVIGATION",fontWeight=FontWeight.Black,fontSize=12.sp,color=Muted)
                Text("Earth  ·  Moon  ·  Mars",fontWeight=FontWeight.Bold,fontSize=18.sp,modifier=Modifier.padding(top=3.dp))
                Text("Next iteration: transfer windows, encounters, docking and station construction.",color=Muted,fontSize=12.sp,modifier=Modifier.padding(top=4.dp))
            }
        }
    }
}

private data class Orbital(val apoapsis:Double,val periapsis:Double)

private fun orbitalMetrics(pos:V2,vel:V2):Orbital{
    val mu=2500.0
    val r=pos.mag()
    val v=vel.mag()
    val energy=v*v/2-mu/r
    if(energy>=0)return Orbital(r+v*25,r-50)
    val a=-mu/(2*energy)
    val h=abs(pos.x*vel.y-pos.y*vel.x)
    val e=sqrt((1-h*h/(a*mu)).coerceAtLeast(0.0))
    return Orbital(a*(1+e),a*(1-e))
}

private fun moonPos(t:Double):V2{
    val a=t*.0045
    return V2(cos(a)*650.0,sin(a)*650.0)
}

private fun step(s:SimState,dt:Double,pitchOffset:Float):SimState{
    if(s.crashed||s.landed)return s
    val moon=moonPos(s.time)
    val muE=2500.0
    val muM=240.0
    val rE=s.pos.mag().coerceAtLeast(1.0)
    val de=s.pos
    val ae=de*(-muE/rE.pow(3))
    val dm=s.pos-moon
    val am=dm*(-muM/dm.mag().coerceAtLeast(1.0).pow(3))
    val gravity=ae+am
    val target=when(s.guidance){
        Guidance.PROGRADE->atan2(s.vel.y,s.vel.x)
        Guidance.RETROGRADE->atan2(-s.vel.y,-s.vel.x)
        Guidance.HOLD_ORBIT->PI/2
        Guidance.MANUAL->pitchOffset*PI/180
    }
    val thrustDir=V2(cos(target),sin(target))
    val activeThrust=if(s.stage<=1)s.thrust else s.thrust*.55
    val burn=activeThrust*s.throttle/s.mass.coerceAtLeast(1.0)
    val thrustAcc=if(s.fuel>0)thrustDir*burn else V2(0.0,0.0)
    val newVel=s.vel+(gravity+thrustAcc)*dt
    val newPos=s.pos+newVel*dt
    val flow=.030*s.throttle*(activeThrust/220.0)
    val newFuel=(s.fuel-flow*dt).coerceAtLeast(0.0)
    val alt=newPos.mag()-50.0
    val speed=newVel.mag()
    val crashed=alt<=0&&speed>2.2
    val landed=alt<=0&&speed<=2.2
    return s.copy(pos=newPos,vel=newVel,angle=target,fuel=newFuel,mass=(s.mass-(s.fuel-newFuel)).coerceAtLeast(1.0),time=s.time+dt,crashed=crashed,landed=landed,trail=(s.trail+newPos).takeLast(520))
}

private fun worldToScreen(p:V2,rocket:V2,center:Offset,scale:Float):Offset{
    return Offset(center.x+((p.x-rocket.x)*scale).toFloat(),center.y-((p.y-rocket.y)*scale).toFloat())
}
