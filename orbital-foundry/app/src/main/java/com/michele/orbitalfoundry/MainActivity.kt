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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
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
        when(screen){
            Screen.HOME->HomeScreen(rocket,missions,{screen=Screen.BUILD},{if(rocket.hasEngine&&rocket.hasCapsule){sim=launchState(rocket);screen=Screen.FLIGHT}},{screen=Screen.MISSIONS})
            Screen.BUILD->BuilderScreen(rocket,{rocket=rocket.copy(parts=rocket.parts+it)},{p->val l=rocket.parts.toMutableList();val i=l.indexOfLast{it==p};if(i>=0)l.removeAt(i);rocket=rocket.copy(parts=l)},{rocket=Rocket(emptyList())},{screen=Screen.HOME},{if(rocket.hasEngine&&rocket.hasCapsule){sim=launchState(rocket);screen=Screen.FLIGHT}})
            Screen.MISSIONS->MissionScreen(missions,{screen=Screen.HOME})
            Screen.FLIGHT->{val s=sim;if(s!=null)FlightScreen(s,{sim=it},{screen=Screen.MAP},{screen=Screen.HOME}){name->missions=missions+name}}
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
private fun HomeScreen(rocket:Rocket,missions:Set<String>,onBuild:()->Unit,onLaunch:()->Unit,onMissions:()->Unit){
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
                Text("A serious mobile flight loop",fontSize=18.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=4.dp))
                Text("Readable editor, telemetry-first cockpit, trajectory trail, staging and orbital guidance without desktop-style controls.",color=Muted,fontSize=13.sp,modifier=Modifier.padding(top=4.dp))
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
private fun BuilderScreen(rocket:Rocket,onAdd:(PartType)->Unit,onRemove:(PartType)->Unit,onClear:()->Unit,onBack:()->Unit,onLaunch:()->Unit){
    Shell("Vehicle Lab","Tap parts to add · stack them into a launcher",onBack){
        Row(verticalAlignment=Alignment.Top,modifier=Modifier.fillMaxWidth()){
            Surface(color=Panel,shape=RoundedCornerShape(20.dp),modifier=Modifier.weight(1f).height(420.dp)){
                Box(Modifier.fillMaxSize(),contentAlignment=Alignment.BottomCenter){
                    Canvas(Modifier.fillMaxSize()){
                        drawCircle(Color(0xFF1B2A45),size.minDimension*.42f,Offset(size.width/2,size.height*.62f),alpha=.35f)
                        var y=size.height*.62f
                        rocket.parts.asReversed().forEach{part->
                            drawRoundRect(part.color,topLeft=Offset(size.width*.34f,y),size=Size(size.width*.32f,42f),cornerRadius=CornerRadius(8f))
                            y-=48f
                        }
                    }
                    Text(if(rocket.parts.isEmpty())"ADD A CORE" else "STACK PREVIEW",color=Muted,fontSize=11.sp,modifier=Modifier.padding(bottom=10.dp))
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.width(145.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                PartType.entries.forEach{p->
                    Surface(color=Panel2,shape=RoundedCornerShape(14.dp),modifier=Modifier.fillMaxWidth().clickable{onAdd(p)}){
                        Column(Modifier.padding(9.dp)){
                            Text(p.emoji+"  "+p.title,fontWeight=FontWeight.Bold,fontSize=12.sp)
                            Text("mass %.1f".format(p.mass),fontSize=9.sp,color=Muted)
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
                        Text(p.emoji,color=p.color,fontSize=18.sp)
                        Column(Modifier.weight(1f).padding(start=8.dp)){Text(p.title,fontWeight=FontWeight.Bold,fontSize=13.sp);Text("mass %.1f · fuel %.1f · thrust %.0f".format(p.mass,p.fuel,p.thrust),color=Muted,fontSize=9.sp)}
                        Text("−",fontSize=22.sp,color=Red,modifier=Modifier.clickable{onRemove(p)})
                    }
                }
            }
        }
    }
}

@Composable
private fun MissionScreen(missions:Set<String>,onBack:()->Unit){
    val all=listOf(
        Triple("Reach 100 km","Suborbital hop · prove the vehicle",Green),
        Triple("Stable Orbit","Complete a closed Earth orbit",Cyan),
        Triple("Moonshot","Cross lunar sphere of influence",Violet),
        Triple("Soft Landing","Touch down below 2 m/s",Orange),
        Triple("Return Home","Re-enter and survive",Red)
    )
    Shell("Mission Control","Contracts reward precision, not chaos",onBack){
        LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){
            items(all){m->
                val done=m.first in missions
                Surface(color=Panel,shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth()){
                    Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.padding(16.dp)){
                        Text(if(done)"✓" else "○",fontSize=28.sp,color=if(done)Green else m.third)
                        Column(Modifier.weight(1f).padding(start=12.dp)){Text(m.first,fontWeight=FontWeight.Black);Text(m.second,color=Muted,fontSize=12.sp)}
                        if(done)Text("DONE",color=Green,fontSize=10.sp,fontWeight=FontWeight.Black)
                    }
                }
            }
        }
    }
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
        alt>650->"MOONSHOT"
        alt>50&&orbit.periapsis>50->"STABLE ORBIT"
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
                repeat(18){i->drawCircle(Color.White,1f,Offset(((i*83)%100)/100f*size.width,((i*47)%100)/100f*size.height),alpha=.35f)}
                val earthR=50*scale
                drawCircle(Color(0xFF264B72),earthR,center)
                drawCircle(Color(0xFF76B9E7),earthR*.92f,center,style=Stroke(width=2f),alpha=.35f)
                if(sim.trail.size>1){
                    val path=Path()
                    sim.trail.forEachIndexed{idx,p->
                        val q=worldToScreen(p,sim.pos,center,scale)
                        if(idx==0)path.moveTo(q.x,q.y) else path.lineTo(q.x,q.y)
                    }
                    drawPath(path,color=Cyan,style=Stroke(width=2.5f),alpha=.8f)
                }
                val rp=worldToScreen(sim.pos,sim.pos,center,scale)
                drawCircle(Color.White,7f,rp)
                drawLine(rp,Offset(rp.x+cos(pitchOffset*PI/180).toFloat()*30f,rp.y-sin(pitchOffset*PI/180).toFloat()*30f),color=Orange,strokeWidth=4f)
                val moon=moonPos(sim.time)
                val ms=worldToScreen(moon,sim.pos,center,scale)
                drawCircle(Color(0xFF9B9EA8),13f,ms)
            }
        }
        Text("THROTTLE "+(sim.throttle*100).roundToInt()+"%",color=Muted,fontSize=10.sp,fontWeight=FontWeight.Bold)
        Slider(value=sim.throttle.toFloat(),onValueChange={onTick(sim.copy(throttle=it.toDouble()))},valueRange=0f..1f,colors=SliderDefaults.colors(thumbColor=Cyan,activeTrackColor=Cyan))
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp),modifier=Modifier.fillMaxWidth()){
            SmallButton(if(paused)"RESUME" else "PAUSE",{paused=!paused},Modifier.weight(1f))
            SmallButton("STAGE",{onTick(sim.copy(stage=sim.stage+1))},Modifier.weight(1f))
            SmallButton("PRO",{onTick(sim.copy(guidance=Guidance.PROGRADE))},Modifier.weight(1f))
            SmallButton("RET",{onTick(sim.copy(guidance=Guidance.RETROGRADE))},Modifier.weight(1f))
            SmallButton("WARP x"+timeWarp,{timeWarp=if(timeWarp==1)5 else if(timeWarp==5)20 else 1},Modifier.weight(1f))
        }
        if(sim.crashed){
            Surface(color=Color(0xEE1A0A11),shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth().padding(top=8.dp)){Text("VEHICLE LOST · RECOVERY REQUIRED",color=Red,fontWeight=FontWeight.Black,modifier=Modifier.padding(14.dp))}
        }else if(sim.landed){
            Surface(color=Color(0xEE0D2A1F),shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth().padding(top=8.dp).clickable{onMission("Soft Landing")} ){Text("SOFT LANDING · TAP TO CLAIM MISSION",color=Green,fontWeight=FontWeight.Black,modifier=Modifier.padding(14.dp))}
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
