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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.foundation.border
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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

enum class Screen{HOME,BUILD,MISSIONS,FLIGHT,MAP,TUTORIAL}
enum class Guidance{MANUAL,PROGRADE,RETROGRADE,HOLD_ORBIT}

enum class PartType(
    val title:String,val emoji:String,val mass:Double,val fuel:Double,val thrust:Double,
    val color:Color,val ispSec:Double=0.0
){
    NOSE("Nose Cone","◢",1.5,0.0,0.0,Violet),
    FAIRING("Payload Fairing","△",0.9,0.0,0.0,Color(0xFF9A7BFF)),
    CAPSULE("Crew Capsule","◉",3.5,0.0,0.0,Cyan),
    PROBE_CORE("Probe Core","◈",0.8,0.0,0.0,Color(0xFF8AD4FF)),
    TANK("Fuel Tank","▣",4.0,24.0,0.0,Color(0xFF6C7BFF)),
    ENGINE("Vector Engine","▲",3.2,0.0,1250.0,Orange,330.0),
    VACUUM_ENGINE("Vector Vacuum Engine","▲",3.2,0.0,2000.0,Cyan,520.0),
    HEAVY_ENGINE("Heavy Lift Engine","▼",8.5,0.0,4000.0,Color(0xFFFF875A),330.0),
    ION_ENGINE("Ion Thruster","✣",0.8,0.0,2.5,Color(0xFF68E7FF),2200.0),
    ION_TANK("Xenon Tank","▣",0.5,6.0,0.0,Color(0xFF4DD5D0)),
    DECOUPLER("Stage Decoupler","⊙",0.8,0.0,0.0,Red),
    FIN("Control Fins","⌁",1.0,0.0,0.0,Green),
    PARACHUTE("Recovery Parachute","⬙",1.2,0.0,0.0,Color(0xFFFF7D90)),
    HEATSHIELD("Heat Shield","⬡",1.8,0.0,0.0,Color(0xFFFF9259)),
    LANDING_LEGS("Landing Legs","⌁",1.2,0.0,0.0,Color(0xFFC4D5E6)),
    RCS("RCS Thrusters","✣",0.6,0.0,0.0,Color(0xFFB69CFF)),
    DOCKING_PORT("Docking Port","⊕",0.8,0.0,0.0,Color(0xFF67F3D0)),
    SOLAR("Solar Panels","✦",0.7,0.0,0.0,Color(0xFFFFD54F))
}

data class Rocket(val parts:List<PartType>){
    val dryMass get()=parts.sumOf{it.mass}
    val fuel get()=parts.sumOf{it.fuel}
    val thrust get()=parts.sumOf{it.thrust}
    val hasEngine get()=parts.any{it==PartType.ENGINE||it==PartType.VACUUM_ENGINE||it==PartType.HEAVY_ENGINE||it==PartType.ION_ENGINE}
    val hasCapsule get()=parts.any{it==PartType.CAPSULE}
    val hasDockingPort get()=parts.any{it==PartType.DOCKING_PORT}
    val hasRcs get()=parts.any{it==PartType.RCS}
    val hasParachute get()=parts.any{it==PartType.PARACHUTE}
    val deltaV get():Double{
        var remaining=parts
        var dv=0.0
        while(remaining.isNotEmpty()){
            val separator=remaining.indexOfLast{it==PartType.DECOUPLER}
            val active=activeStageParts(remaining)
            val wet=remaining.sumOf{it.mass+it.fuel}
            val propellant=active.sumOf{it.fuel}
            val engines=active.filter{it.thrust>0.0}
            val totalThrust=engines.sumOf{it.thrust}
            val isp=if(totalThrust>0.0)engines.sumOf{it.thrust*it.ispSec}/totalThrust else 0.0
            if(totalThrust>0.0&&isp>0.0&&propellant>0.0&&wet>propellant){
                dv+=(isp*G0/1000.0)*ln(wet/(wet-propellant))
            }
            if(separator<0)break
            val next=remaining.take(separator)
            if(next.size>=remaining.size)break
            remaining=next
        }
        return dv
    }
}
fun starterRocket():Rocket=Rocket(listOf(
    PartType.NOSE,PartType.CAPSULE,PartType.HEATSHIELD,PartType.PARACHUTE,
    PartType.TANK,PartType.TANK,PartType.VACUUM_ENGINE,PartType.DECOUPLER,
    PartType.TANK,PartType.TANK,PartType.VACUUM_ENGINE,PartType.DECOUPLER,
    PartType.TANK,PartType.TANK,PartType.VACUUM_ENGINE,PartType.DECOUPLER,
    PartType.TANK,PartType.TANK,PartType.ENGINE,PartType.ENGINE,PartType.ENGINE,PartType.ENGINE,PartType.FIN
))

data class SimState(
    val pos:V2=V2(0.0,EARTH_RADIUS_KM+0.02),
    val vel:V2=V2(0.0,0.0),
    val angle:Double=PI/2,
    val throttle:Double=1.0,
    val fuel:Double,
    val mass:Double,
    val thrust:Double,
    val parts:List<PartType>,
    val time:Double=0.0,
    val stage:Int=1,
    val guidance:Guidance=Guidance.MANUAL,
    val crashed:Boolean=false,
    val landed:Boolean=false,
    val heat:Double=0.0,
    val maxDynamicPressureKpa:Double=0.0,
    val maxAltitudeKm:Double=0.0,
    val orbitProgressRadians:Double=0.0,
    val lastOrbitalAngle:Double?=null,
    val completedOrbit:Boolean=false,
    val parachuteDeployed:Boolean=false,
    val targetPos:V2?=null,
    val targetVel:V2?=null,
    val docking:Boolean=false,
    val docked:Boolean=false,
    val trail:List<V2> = emptyList()
)

class MainActivity:ComponentActivity(){
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        val preferences=getSharedPreferences("orbital_foundry", MODE_PRIVATE)
        val showTutorial=!preferences.getBoolean("tutorial_seen",false)
        val savedParts=preferences.getString("rocket_parts",null)
        val restoredParts=savedParts?.split(",")?.mapNotNull{token->
            runCatching{PartType.valueOf(token)}.getOrNull()
        }.orEmpty()
        val initialRocket=if(restoredParts.isNotEmpty())Rocket(restoredParts)else starterRocket()
        val defaults=CareerState()
        val initialCareer=CareerState(
            funds=preferences.getInt("career_funds",defaults.funds),
            science=preferences.getInt("career_science",defaults.science),
            reputation=preferences.getInt("career_reputation",defaults.reputation),
            unlocked=preferences.getStringSet("career_unlocked",null)?.toSet()?:defaults.unlocked,
            completedContracts=preferences.getStringSet("career_contracts",null)?.toSet().orEmpty()
        )
        val initialMissions=preferences.getStringSet("flight_achievements",null)?.toSet().orEmpty()
        setContent{
            Box(Modifier.fillMaxSize().background(Bg).windowInsetsPadding(WindowInsets.safeDrawing)){
                OrbitalFoundryApp(
                    showTutorialOnStart=showTutorial,
                    initialRocket=initialRocket,
                    initialMissions=initialMissions,
                    initialCareer=initialCareer,
                    onTutorialComplete={preferences.edit().putBoolean("tutorial_seen",true).apply()},
                    onSaveRocket={rocket->preferences.edit().putString("rocket_parts",rocket.parts.joinToString(","){it.name}).apply()},
                    onSaveMissions={missions->preferences.edit().putStringSet("flight_achievements",missions.toMutableSet()).apply()},
                    onSaveCareer={career->
                        preferences.edit()
                            .putInt("career_funds",career.funds)
                            .putInt("career_science",career.science)
                            .putInt("career_reputation",career.reputation)
                            .putStringSet("career_unlocked",career.unlocked.toMutableSet())
                            .putStringSet("career_contracts",career.completedContracts.toMutableSet())
                            .apply()
                    }
                )
            }
        }
    }
}

@Composable
fun OrbitalFoundryApp(
    showTutorialOnStart:Boolean,
    initialRocket:Rocket,
    initialMissions:Set<String>,
    initialCareer:CareerState,
    onTutorialComplete:()->Unit,
    onSaveRocket:(Rocket)->Unit,
    onSaveMissions:(Set<String>)->Unit,
    onSaveCareer:(CareerState)->Unit
){
    MaterialTheme(colorScheme=darkColorScheme(background=Bg,surface=Panel,primary=Violet,onBackground=Ink,onSurface=Ink)){
        var screen by rememberSaveable{mutableStateOf(if(showTutorialOnStart)Screen.TUTORIAL else Screen.HOME)}
        var rocket by remember{mutableStateOf(initialRocket)}
        var sim by remember{mutableStateOf<SimState?>(null)}
        var missions by remember{mutableStateOf(initialMissions)}
        var career by remember{mutableStateOf(initialCareer)}
        LaunchedEffect(rocket){onSaveRocket(rocket)}
        LaunchedEffect(missions){onSaveMissions(missions)}
        LaunchedEffect(career){onSaveCareer(career)}
        when(screen){
            Screen.HOME->HomeScreen(rocket,missions,career,
                {screen=Screen.BUILD},
                {if(rocket.hasEngine&&rocket.hasCapsule){sim=launchState(rocket);screen=Screen.FLIGHT}},
                {screen=Screen.MISSIONS},
                {screen=Screen.TUTORIAL},
                {if(rocket.hasDockingPort&&rocket.hasRcs){sim=beginDockingPractice(rocket);screen=Screen.FLIGHT}}
            )
            Screen.BUILD->BuilderScreen(rocket,career.unlocked,
                {p->rocket=addPartInUsefulPosition(rocket,p)},
                {rocket=addUpperStage(rocket)},
                {index->val l=rocket.parts.toMutableList();if(index in l.indices)l.removeAt(index);rocket=rocket.copy(parts=l)},
                {index,delta->
                    val l=rocket.parts.toMutableList()
                    val destination=(index+delta).coerceIn(0,l.lastIndex)
                    if(destination!=index){val part=l.removeAt(index);l.add(destination,part);rocket=rocket.copy(parts=l)}
                },
                {rocket=Rocket(emptyList())},
                {screen=Screen.HOME},
                {if(rocket.hasEngine&&rocket.hasCapsule){sim=launchState(rocket);screen=Screen.FLIGHT}}
            )
            Screen.MISSIONS->MissionScreen(missions,career,{career=it},{screen=Screen.HOME})
            Screen.FLIGHT->{val s=sim;if(s!=null)FlightScreen(s,
                {next->sim=next;missions=missions+missionCompletions(next)},
                {screen=Screen.MAP},{screen=Screen.HOME},
                {name->missions=missions+name}
            )}
            Screen.MAP->{val s=sim;if(s!=null)MapScreen(s,{screen=Screen.FLIGHT})}
            Screen.TUTORIAL->TutorialScreen({onTutorialComplete();screen=Screen.HOME},{onTutorialComplete();screen=Screen.HOME})
        }
    }
}

private fun requiredTech(part:PartType):String?=when(part){
    PartType.NOSE->null
    PartType.CAPSULE,PartType.PARACHUTE,PartType.HEATSHIELD->"capsule"
    PartType.TANK->"small_tank"
    PartType.ENGINE,PartType.DECOUPLER,PartType.FIN->"starter_engine"
    PartType.VACUUM_ENGINE->"vacuum"
    PartType.HEAVY_ENGINE->"heavy"
    PartType.ION_ENGINE,PartType.ION_TANK->"ion"
    PartType.FAIRING,PartType.PROBE_CORE->"fairing"
    PartType.LANDING_LEGS->"landing"
    PartType.RCS->"rcs"
    PartType.DOCKING_PORT->"docking"
    PartType.SOLAR->"solar"
}

private fun isPartUnlocked(part:PartType,unlocked:Set<String>):Boolean =
    requiredTech(part)?.let{it in unlocked} ?: true

private fun addPartInUsefulPosition(rocket:Rocket,part:PartType):Rocket{
    val list=rocket.parts.toMutableList()
    when(part){
        PartType.DECOUPLER->{ return addUpperStage(rocket) }
        PartType.NOSE->list.add(0,part)
        PartType.CAPSULE->{
            val nose=list.indexOfLast{it==PartType.NOSE}
            list.add(if(nose>=0)nose+1 else 0,part)
        }
        PartType.FAIRING->{
            val capsule=list.indexOfFirst{it==PartType.CAPSULE}
            val nose=list.indexOfLast{it==PartType.NOSE}
            list.add(if(capsule>=0)capsule else if(nose>=0)nose+1 else 0,part)
        }
        PartType.TANK,PartType.ENGINE,PartType.VACUUM_ENGINE,PartType.HEAVY_ENGINE,PartType.ION_ENGINE,PartType.ION_TANK,PartType.FIN->list.add(part)
        PartType.DOCKING_PORT,PartType.RCS,PartType.PROBE_CORE,PartType.SOLAR,PartType.PARACHUTE,PartType.HEATSHIELD,PartType.LANDING_LEGS->{
            val firstTank=list.indexOfFirst{it==PartType.TANK}
            val insertAt=if(firstTank>=0)firstTank else list.size
            list.add(insertAt,part)
        }
    }
    return rocket.copy(parts=list)
}

private fun addUpperStage(rocket:Rocket):Rocket{
    val list=rocket.parts.toMutableList()
    val firstTank=list.indexOfFirst{it==PartType.TANK}
    val firstEngine=list.indexOfFirst{it==PartType.ENGINE}
    val at=when{
        firstTank>=0->firstTank
        firstEngine>=0->firstEngine
        else->list.size
    }
    list.addAll(at,listOf(PartType.TANK,PartType.TANK,PartType.VACUUM_ENGINE,PartType.DECOUPLER))
    return rocket.copy(parts=list)
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
private fun HomeScreen(
    rocket:Rocket,missions:Set<String>,career:CareerState,
    onBuild:()->Unit,onLaunch:()->Unit,onMissions:()->Unit,onTutorial:()->Unit,onDocking:()->Unit
){
    Column(Modifier.fillMaxSize().background(Bg).padding(horizontal=16.dp, vertical=12.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){
            Column(Modifier.weight(1f)){
                Text("ORBITAL",fontSize=38.sp,fontWeight=FontWeight.Black,lineHeight=38.sp)
                Text("FOUNDRY",fontSize=18.sp,fontWeight=FontWeight.Bold,color=Violet)
                Text("ROCKET ENGINEERING SIMULATOR",color=Muted,fontSize=11.sp,letterSpacing=1.sp)
            }
            Box(Modifier.size(56.dp).background(Panel2,CircleShape),contentAlignment=Alignment.Center){
                RocketPartThumbnail(PartType.ENGINE)
            }
        }
        Spacer(Modifier.height(12.dp))
        Surface(color=Panel,shape=RoundedCornerShape(22.dp),modifier=Modifier.fillMaxWidth()){
            Column(Modifier.padding(14.dp)){
                Row(verticalAlignment=Alignment.CenterVertically){
                    Column(Modifier.weight(1f)){
                        Text("FLIGHT VEHICLE",color=Green,fontSize=11.sp,fontWeight=FontWeight.Black,letterSpacing=1.sp)
                        Text(if(rocket.hasEngine&&rocket.hasCapsule)"Ready for launch" else "Complete the vehicle",fontSize=20.sp,fontWeight=FontWeight.Black)
                    }
                    Surface(color=Panel2,shape=RoundedCornerShape(12.dp)){
                        Text("${rocket.parts.size} PARTS",modifier=Modifier.padding(horizontal=10.dp,vertical=7.dp),fontSize=11.sp,fontWeight=FontWeight.Bold,color=Cyan)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement=Arrangement.spacedBy(7.dp),modifier=Modifier.fillMaxWidth()){
                    Metric("FUNDS","€${career.funds/1000}K",Modifier.weight(1f))
                    Metric("SCIENCE","${career.science}",Modifier.weight(1f))
                    Metric("TECH","${career.unlocked.size}",Modifier.weight(1f))
                }
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement=Arrangement.spacedBy(7.dp),modifier=Modifier.fillMaxWidth()){
                    Metric("WET MASS","%.1f t".format(rocket.dryMass+rocket.fuel),Modifier.weight(1f))
                    Metric("PROPELLANT","%.0f t".format(rocket.fuel),Modifier.weight(1f))
                    Metric("TOTAL ΔV","%.2f km/s".format(rocket.deltaV),Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick=onLaunch,enabled=rocket.hasEngine&&rocket.hasCapsule,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp),shape=RoundedCornerShape(14.dp)){
                    Text("LAUNCH MISSION",fontWeight=FontWeight.Black,fontSize=14.sp)
                }
                OutlinedButton(onClick=onBuild,modifier=Modifier.fillMaxWidth().heightIn(min=46.dp),shape=RoundedCornerShape(14.dp)){
                    Text("VEHICLE ASSEMBLY",fontWeight=FontWeight.Bold,fontSize=13.sp)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(9.dp),modifier=Modifier.fillMaxWidth()){
            ActionCard("⌘","VEHICLE LAB","Parts · stages · ΔV",onBuild,Modifier.weight(1f))
            ActionCard("◈","MISSIONS","${missions.size} objectives complete",onMissions,Modifier.weight(1f))
        }
        Spacer(Modifier.height(9.dp))
        OutlinedButton(
            onClick=onDocking, enabled=rocket.hasDockingPort&&rocket.hasRcs,
            modifier=Modifier.fillMaxWidth().heightIn(min=46.dp),shape=RoundedCornerShape(14.dp)
        ){
            Text(if(rocket.hasDockingPort&&rocket.hasRcs)"OPEN ORBITAL DOCKING RANGE" else "DOCKING RANGE · ADD RCS + DOCKING PORT",fontSize=12.sp,fontWeight=FontWeight.Bold)
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth()){
            OutlinedButton(onClick=onTutorial,modifier=Modifier.weight(1f).heightIn(min=42.dp),shape=RoundedCornerShape(12.dp)){
                Text("FLIGHT TUTORIAL",fontSize=11.sp,fontWeight=FontWeight.Bold)
            }
            OutlinedButton(onClick=onMissions,modifier=Modifier.weight(1f).heightIn(min=42.dp),shape=RoundedCornerShape(12.dp)){
                Text("CAREER / TECH TREE",fontSize=11.sp,fontWeight=FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(7.dp))
        Surface(color=Panel2,shape=RoundedCornerShape(16.dp),modifier=Modifier.fillMaxWidth()){
            Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){
                Text("●",color=Cyan,fontSize=13.sp)
                Spacer(Modifier.width(9.dp))
                Column{
                    Text("FLIGHT COMPUTER",fontSize=11.sp,fontWeight=FontWeight.Black,letterSpacing=.8.sp)
                    Text("Atmospheric drag · staged propellant · orbital elements · re-entry heating",fontSize=11.sp,color=Muted,lineHeight=15.sp)
                }
            }
        }
    }
}

@Composable
private fun Metric(label:String,value:String,modifier:Modifier=Modifier){
    Surface(color=Panel2,shape=RoundedCornerShape(14.dp),modifier=modifier){
        Column(Modifier.padding(horizontal=7.dp, vertical=9.dp),horizontalAlignment=Alignment.CenterHorizontally){
            Text(label,color=Muted,fontSize=10.sp,fontWeight=FontWeight.Bold,lineHeight=12.sp)
            Text(value,fontSize=14.sp,fontWeight=FontWeight.Black,lineHeight=17.sp)
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
        val partH=min(38f,(size.height-104f)/count).coerceIn(10f,38f)
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
        PartType.FAIRING->{
            val fairing=Path().apply{
                moveTo(cx,top)
                cubicTo(left+width*.95f,top+height*.16f,left+width*.90f,top+height*.68f,left+width*.82f,top+height*.88f)
                lineTo(left+width*.18f,top+height*.88f)
                cubicTo(left+width*.10f,top+height*.68f,left+width*.05f,top+height*.16f,cx,top)
                close()
            }
            drawPath(fairing,Brush.horizontalGradient(listOf(Color(0xFF353D51),Color(0xFFF5F7FD),Color(0xFFBCAFFF),Color(0xFF303849)),left,left+width))
            drawPath(fairing,edge,style=Stroke(1.5f))
            drawLine(Color(0xFF45D7FF),Offset(cx,top+height*.16f),Offset(cx,top+height*.78f),1.2f)
            drawRoundRect(Color(0xFF101726),Offset(left+width*.20f,top+height*.72f),Size(width*.60f,height*.12f),CornerRadius(2f))
        }
        PartType.CAPSULE->{
            val p=Path().apply{moveTo(cx,top);cubicTo(left+width*.86f,top+height*.05f,left+width*.94f,top+height*.24f,left+width*.88f,top+height*.50f);lineTo(left+width*.84f,top+height*.92f);lineTo(left+width*.16f,top+height*.92f);lineTo(left+width*.12f,top+height*.50f);cubicTo(left+width*.06f,top+height*.24f,left+width*.14f,top+height*.05f,cx,top);close()}
            drawPath(p,metal);drawPath(p,edge,style=Stroke(1.6f))
            drawRoundRect(Color(0xFF182638),Offset(cx-width*.22f,top+height*.30f),Size(width*.44f,height*.25f),CornerRadius(height*.12f))
            drawCircle(Color(0xFF69D9FF),height*.08f,Offset(cx,top+height*.42f))
            drawRoundRect(Color(0xFFB8C6D8),Offset(left+width*.10f,top+height*.82f),Size(width*.80f,height*.10f),CornerRadius(2f))
        }
        PartType.TANK,PartType.ION_TANK->{
            drawRoundRect(metal,Offset(left+width*.07f,top+height*.02f),Size(width*.86f,height*.96f),CornerRadius(width*.11f))
            drawRoundRect(edge,Offset(left+width*.07f,top+height*.02f),Size(width*.86f,height*.96f),CornerRadius(width*.11f),style=Stroke(1.4f))
            for(i in 0..3){
                val y=top+height*(.10f+i*.25f)
                drawLine(Color(0xFF1B2739),Offset(left+width*.11f,y),Offset(left+width*.89f,y),2f)
                drawLine(Color(0x99FFFFFF),Offset(left+width*.16f,y+2f),Offset(left+width*.84f,y+2f),.9f)
            }
            drawRoundRect(Color(0xFFD8E4EF),Offset(left+width*.41f,top+height*.19f),Size(width*.18f,height*.46f),CornerRadius(2f))
            drawLine(if(part==PartType.ION_TANK)Color(0xFF67F3D0) else Color(0xFFFFC46E),Offset(left+width*.20f,top+height*.69f),Offset(left+width*.80f,top+height*.69f),2f)
        }
        PartType.ENGINE,PartType.VACUUM_ENGINE,PartType.HEAVY_ENGINE,PartType.ION_ENGINE->{
            val nozzleWidth=if(part==PartType.HEAVY_ENGINE).78f else if(part==PartType.ION_ENGINE).25f else .60f
            drawRoundRect(metal,Offset(left+(1f-nozzleWidth)*.5f*width,top),Size(width*nozzleWidth,height*.34f),CornerRadius(2f))
            val nozzle=Path().apply{moveTo(left+width*.27f,top+height*.28f);lineTo(left+width*.73f,top+height*.28f);lineTo(left+width*.92f,top+height*.86f);quadraticTo(cx,top+height*1.08f,left+width*.08f,top+height*.86f);close()}
            drawPath(nozzle,Brush.horizontalGradient(listOf(Color(0xFF171E2B),Color(0xFFBAC7D8),Color(0xFF354154),Color(0xFF0D1421)),left,left+width))
            drawPath(nozzle,edge,style=Stroke(1.5f))
            drawOval(Color(0xFF05070C),Offset(left+width*.22f,top+height*.72f),Size(width*.56f,height*.18f))
            drawOval(if(part==PartType.VACUUM_ENGINE||part==PartType.ION_ENGINE)Color(0xFF45D7FF) else if(part==PartType.HEAVY_ENGINE)Color(0xFFFF734A) else Color(0xFFFFA53F),Offset(left+width*.35f,top+height*.76f),Size(width*.30f,height*.10f))
            drawLine(if(part==PartType.VACUUM_ENGINE||part==PartType.ION_ENGINE)Color(0xFF9CF2FF) else Color(0xFFFFD27F),Offset(cx,top+height*.79f),Offset(cx,top+height*.90f),2f)
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
        PartType.PROBE_CORE->{
            drawRoundRect(metal,Offset(left+width*.16f,top+height*.10f),Size(width*.68f,height*.80f),CornerRadius(4f))
            drawRoundRect(Color(0xFF101829),Offset(left+width*.26f,top+height*.22f),Size(width*.48f,height*.30f),CornerRadius(3f))
            drawCircle(Color(0xFF69D9FF),height*.07f,Offset(cx,top+height*.37f))
            for(i in 0..2)drawLine(Color(0xFF9CB2CD),Offset(left+width*.25f,top+height*(.65f+i*.07f)),Offset(left+width*.75f,top+height*(.65f+i*.07f)),1f)
        }
        PartType.PARACHUTE->{
            drawRoundRect(metal,Offset(left+width*.33f,top+height*.52f),Size(width*.34f,height*.38f),CornerRadius(3f))
            val canopy=Path().apply{moveTo(left+width*.10f,top+height*.43f);quadraticTo(cx,top-height*.05f,left+width*.90f,top+height*.43f);lineTo(left+width*.76f,top+height*.48f);quadraticTo(cx,top+height*.20f,left+width*.24f,top+height*.48f);close()}
            drawPath(canopy,Brush.horizontalGradient(listOf(Color(0xFFB8234B),Color(0xFFFF7D90),Color(0xFFFFD9E2)),left,left+width))
            drawPath(canopy,edge,style=Stroke(1.2f))
            for(i in 0..4)drawLine(Color(0xFFE9F3FF),Offset(left+width*(.16f+i*.17f),top+height*.42f),Offset(cx,top+height*.57f),1f)
        }
        PartType.HEATSHIELD->{
            val shield=Path().apply{moveTo(left+width*.08f,top+height*.15f);lineTo(left+width*.92f,top+height*.15f);lineTo(left+width*.78f,top+height*.65f);quadraticTo(cx,top+height*1.05f,left+width*.22f,top+height*.65f);close()}
            drawPath(shield,Brush.verticalGradient(listOf(Color(0xFFFFD19A),Color(0xFFCF643F),Color(0xFF4A2929)),top,top+height))
            drawPath(shield,edge,style=Stroke(1.5f))
            for(i in 1..3)drawLine(Color(0xFFFFB17A),Offset(left+width*.22f,top+height*(.2f+i*.12f)),Offset(left+width*.78f,top+height*(.2f+i*.12f)),1.1f)
        }
        PartType.LANDING_LEGS->{
            drawRoundRect(metal,Offset(cx-width*.16f,top+height*.08f),Size(width*.32f,height*.65f),CornerRadius(3f))
            val a=Path().apply{moveTo(cx-width*.10f,top+height*.55f);lineTo(left-width*.04f,top+height*.95f);lineTo(left+width*.35f,top+height*.87f);lineTo(cx-width*.08f,top+height*.62f);close()}
            val b=Path().apply{moveTo(cx+width*.10f,top+height*.55f);lineTo(left+width*1.04f,top+height*.95f);lineTo(left+width*.65f,top+height*.87f);lineTo(cx+width*.08f,top+height*.62f);close()}
            drawPath(a,Color(0xFFB9C7D8));drawPath(b,Color(0xFFB9C7D8))
            drawLine(Color.White,Offset(left-width*.04f,top+height*.95f),Offset(left+width*.35f,top+height*.87f),2f)
            drawLine(Color.White,Offset(left+width*1.04f,top+height*.95f),Offset(left+width*.65f,top+height*.87f),2f)
        }
        PartType.RCS->{
            drawRoundRect(metal,Offset(cx-width*.18f,top+height*.16f),Size(width*.36f,height*.68f),CornerRadius(3f))
            drawRoundRect(Color(0xFFBAC9DB),Offset(left+width*.08f,top+height*.28f),Size(width*.84f,height*.18f),CornerRadius(3f))
            drawRoundRect(Color(0xFFBAC9DB),Offset(left+width*.08f,top+height*.60f),Size(width*.84f,height*.18f),CornerRadius(3f))
            drawCircle(Color(0xFF9A7BFF),height*.10f,Offset(left+width*.07f,top+height*.37f))
            drawCircle(Color(0xFF9A7BFF),height*.10f,Offset(left+width*.93f,top+height*.69f))
        }
        PartType.DOCKING_PORT->{
            drawRoundRect(metal,Offset(left+width*.26f,top+height*.10f),Size(width*.48f,height*.80f),CornerRadius(3f))
            drawOval(Color(0xFF0B111D),Offset(left+width*.08f,top+height*.25f),Size(width*.84f,height*.52f))
            drawOval(Brush.horizontalGradient(listOf(Color(0xFF60778D),Color(0xFFE1EDF8),Color(0xFF60778D)),left,left+width),Offset(left+width*.14f,top+height*.27f),Size(width*.72f,height*.48f),style=Stroke(3f))
            drawOval(Color(0xFF0C1524),Offset(left+width*.30f,top+height*.37f),Size(width*.40f,height*.28f))
            drawCircle(Color(0xFF45E0A8),height*.055f,Offset(cx,top+height*.51f))
        }
    }
}

@Composable
private fun BuilderScreen(rocket:Rocket,unlockedTech:Set<String>,onAdd:(PartType)->Unit,onAddStage:()->Unit,onRemove:(Int)->Unit,onMove:(Int,Int)->Unit,onClear:()->Unit,onBack:()->Unit,onLaunch:()->Unit){
    Shell("Vehicle Lab","Tap parts to add · stack them into a launcher",onBack){
        Row(verticalAlignment=Alignment.Top,modifier=Modifier.fillMaxWidth()){
            Surface(color=Panel,shape=RoundedCornerShape(20.dp),modifier=Modifier.weight(1f).height(420.dp)){
                Box(Modifier.fillMaxSize(),contentAlignment=Alignment.BottomCenter){
                    RocketStackPreview(rocket)
                    Text(if(rocket.parts.isEmpty())"ADD A CORE" else "STACK PREVIEW",color=Muted,fontSize=11.sp,modifier=Modifier.padding(bottom=10.dp))
                }
            }
            Spacer(Modifier.width(10.dp))
            LazyColumn(Modifier.width(148.dp).height(420.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                items(PartType.entries.toList()){p->
                    val unlocked=isPartUnlocked(p,unlockedTech)
                    val tileModifier=if(unlocked)Modifier.fillMaxWidth().clickable{onAdd(p)} else Modifier.fillMaxWidth()
                    Surface(color=if(unlocked)Panel2 else Color(0xFF0A0D15),shape=RoundedCornerShape(12.dp),modifier=tileModifier){
                        Row(Modifier.padding(5.dp),verticalAlignment=Alignment.CenterVertically){
                            RocketPartThumbnail(p)
                            Spacer(Modifier.width(5.dp))
                            Column(Modifier.weight(1f)){
                                Text(p.title,fontWeight=FontWeight.Bold,fontSize=11.sp,lineHeight=13.sp,color=if(unlocked)Ink else Muted)
                                Text(if(unlocked)"M %.1f t · F %.0f t".format(p.mass,p.fuel) else "LOCKED · "+(requiredTech(p)?: "").uppercase(),fontSize=9.sp,color=if(unlocked)Muted else Violet,lineHeight=11.sp)
                                if(unlocked&&p.thrust>0)Text("%.0f kN · Isp %.0f s".format(p.thrust,p.ispSec),fontSize=9.sp,color=Orange,lineHeight=11.sp)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp),modifier=Modifier.fillMaxWidth()){
            Column(Modifier.weight(1f).align(Alignment.CenterVertically)){
                Text("STACK · TOP TO BOTTOM",fontWeight=FontWeight.Black,fontSize=10.sp,color=Muted)
                Text("${rocket.parts.size} parts · ${rocket.parts.count{it==PartType.DECOUPLER}+1} stages",fontSize=11.sp,color=Cyan)
            }
            OutlinedButton(onClick=onClear,contentPadding=PaddingValues(horizontal=9.dp,vertical=8.dp)){Text("RESET",fontSize=10.sp)}
            OutlinedButton(onClick=onAddStage,contentPadding=PaddingValues(horizontal=9.dp,vertical=8.dp)){Text("+ STAGE",fontSize=10.sp)}
            Button(onClick=onLaunch,enabled=rocket.hasEngine&&rocket.hasCapsule,contentPadding=PaddingValues(horizontal=12.dp,vertical=8.dp)){Text("LAUNCH",fontSize=10.sp)}
        }
        Spacer(Modifier.height(8.dp))
        Text("STACK",color=Muted,fontSize=11.sp,fontWeight=FontWeight.Black)
        LazyColumn(verticalArrangement=Arrangement.spacedBy(6.dp),modifier=Modifier.weight(1f)){
            itemsIndexed(rocket.parts){visualIndex,p->
                val originalIndex=visualIndex
                Surface(color=Panel,shape=RoundedCornerShape(12.dp),modifier=Modifier.fillMaxWidth()){
                    Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.padding(horizontal=8.dp,vertical=5.dp)){
                        RocketPartThumbnail(p)
                        Column(Modifier.weight(1f).padding(start=7.dp)){
                            Text(p.title,fontWeight=FontWeight.Bold,fontSize=12.sp,lineHeight=14.sp)
                            Text("M %.1f · F %.1f · T %.0f".format(p.mass,p.fuel,p.thrust),color=Muted,fontSize=10.sp,lineHeight=12.sp)
                        }
                        Column(horizontalAlignment=Alignment.CenterHorizontally){
                            Text("↑",fontSize=18.sp,color=Cyan,modifier=Modifier.clickable{onMove(originalIndex,-1)}.padding(horizontal=5.dp))
                            Text("↓",fontSize=18.sp,color=Cyan,modifier=Modifier.clickable{onMove(originalIndex,1)}.padding(horizontal=5.dp))
                        }
                        Text("×",fontSize=20.sp,color=Red,modifier=Modifier.clickable{onRemove(originalIndex)}.padding(horizontal=5.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TutorialScreen(onFinish:()->Unit,onSkip:()->Unit){
    var page by rememberSaveable{mutableIntStateOf(0)}
    val lessons=listOf(
        Triple("01 / BUILD THE STACK","The editor stores the rocket top-to-bottom. The bottom stage is the section below a decoupler. Use ↑ and ↓ to reorder parts; add a tank and an engine for each extra stage.","Start with a capsule, heat shield, parachute, fuel tanks, engines and at least one decoupler."),
        Triple("02 / CHECK THE ENGINEERING","Wet mass includes structure plus every tank. Thrust-to-weight ratio must be above 1.0 to lift off; around 1.3–1.8 is a useful starting point. ΔV is estimated stage by stage using engine Isp.","The model uses kilometres, seconds, tonnes and kilonewtons consistently. Atmospheric drag grows quickly at low altitude."),
        Triple("03 / FLY TO ORBIT","Launch vertically, then use P− to tip toward the horizon. PRO points along velocity; RET points against it. Watch apoapsis and periapsis: a stable orbit needs both above the atmosphere.","Use throttle to manage acceleration and dynamic pressure. Press STAGE only when the active stage is ready to separate."),
        Triple("04 / RE-ENTRY & RECOVERY","Use the heat shield on the capsule. The parachute is intended for low altitude and low speed; deploying it too early at orbital speed can destroy the vehicle.","Thermal load and maximum dynamic pressure are shown by the flight computer. Landing below the safe speed completes recovery."),
        Triple("05 / DOCKING","Add both RCS Thrusters and a Docking Port, then open the Docking Range from the home screen. The training craft starts in a circular 200 km orbit.","Tap APPROACH once to add a small relative velocity. Tap MATCH V near the target to stop relative drift; press DOCK only within 5 m and below 0.5 m/s.")
    )
    val lesson=lessons[page]
    Column(Modifier.fillMaxSize().background(Bg).padding(horizontal=18.dp,vertical=14.dp)){
        Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.fillMaxWidth()){
            Column(Modifier.weight(1f)){
                Text("FLIGHT SCHOOL",fontSize=12.sp,color=Cyan,fontWeight=FontWeight.Black,letterSpacing=1.5.sp)
                Text("Learn to fly",fontSize=30.sp,fontWeight=FontWeight.Black)
            }
            Text("${page+1} / ${lessons.size}",fontSize=13.sp,color=Muted,fontWeight=FontWeight.Bold)
        }
        Spacer(Modifier.height(14.dp))
        Surface(color=Panel,shape=RoundedCornerShape(24.dp),modifier=Modifier.fillMaxWidth().weight(1f)){
            Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                Surface(color=Panel2,shape=RoundedCornerShape(16.dp),modifier=Modifier.fillMaxWidth()){
                    Box(Modifier.height(132.dp).fillMaxWidth(),contentAlignment=Alignment.Center){
                        RocketStackPreview(Rocket(listOf(PartType.NOSE,PartType.CAPSULE,PartType.TANK,PartType.ENGINE,PartType.DECOUPLER,PartType.TANK,PartType.ENGINE,PartType.FIN)))
                    }
                }
                Text(lesson.first,fontSize=12.sp,color=Violet,fontWeight=FontWeight.Black,letterSpacing=1.sp)
                Text(lesson.second,fontSize=17.sp,lineHeight=24.sp,fontWeight=FontWeight.SemiBold)
                Text(lesson.third,fontSize=14.sp,lineHeight=21.sp,color=Muted)
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement=Arrangement.spacedBy(5.dp),modifier=Modifier.fillMaxWidth()){
                    lessons.indices.forEach{idx->
                        Box(Modifier.weight(1f).height(4.dp).background(if(idx<=page)Cyan else Panel2,RoundedCornerShape(4.dp)))
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth()){
            OutlinedButton(onClick=onSkip,modifier=Modifier.weight(1f).heightIn(min=48.dp),shape=RoundedCornerShape(14.dp)){
                Text("SKIP",fontWeight=FontWeight.Bold)
            }
            if(page>0) OutlinedButton(onClick={page--},modifier=Modifier.weight(1f).heightIn(min=48.dp),shape=RoundedCornerShape(14.dp)){
                Text("BACK",fontWeight=FontWeight.Bold)
            }
            Button(onClick={if(page==lessons.lastIndex)onFinish() else page++},modifier=Modifier.weight(1.4f).heightIn(min=48.dp),shape=RoundedCornerShape(14.dp)){
                Text(if(page==lessons.lastIndex)"START FLIGHT" else "NEXT",fontWeight=FontWeight.Black)
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
                val available=contract.availableInBuild && canAccept(contract,career) && contract.id in missions
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
                        else Button(onClick={onCareer(completeContract(contract,career))},enabled=available){Text(if(available)"CLAIM" else if(!contract.availableInBuild)"NEXT PHASE" else if(contract.id in missions)"READY" else "LOCK",fontSize=9.sp)}
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
    val altitude=s.pos.mag()-EARTH_RADIUS_KM
    val orbit=orbitalMetrics(s.pos,s.vel)
    val done=mutableSetOf<String>()
    if(s.landed&&s.maxAltitudeKm>=80.0)done+="suborbital"
    if(!orbit.escape&&orbit.periapsisAltitudeKm>=100.0)done+="orbit"
    if(!orbit.escape&&orbit.periapsisAltitudeKm>=120.0&&altitude>=120.0)done+="satellite"
    if(s.landed)done+="recovery"
    if(s.docked)done+="dock"
    return done
}

private fun launchState(rocket:Rocket):SimState{
    val active=activeStageParts(rocket.parts)
    return SimState(
        pos=V2(0.0,EARTH_RADIUS_KM+0.02),
        vel=V2(0.0,0.0),
        angle=PI/2,
        throttle=1.0,
        fuel=active.sumOf{it.fuel},
        mass=rocket.dryMass+rocket.fuel,
        thrust=active.sumOf{it.thrust},
        parts=rocket.parts,
        trail=listOf(V2(0.0,EARTH_RADIUS_KM+0.02))
    )
}

@Composable
private fun FlightScreen(sim:SimState,onTick:(SimState)->Unit,onMap:()->Unit,onBack:()->Unit,onMission:(String)->Unit){
    var paused by rememberSaveable{mutableStateOf(false)}
    var pitchOffset by rememberSaveable{mutableFloatStateOf(90f)}
    var timeWarp by rememberSaveable{mutableIntStateOf(1)}
    val latestSim by rememberUpdatedState(sim)
    val latestPitch by rememberUpdatedState(pitchOffset)
    LaunchedEffect(paused,timeWarp,sim.crashed,sim.landed,sim.docked){
        if(paused||sim.crashed||sim.landed||sim.docked)return@LaunchedEffect
        while(true){
            delay(40L)
            val current=latestSim?:break
            if(current.crashed||current.landed||current.docked)break
            onTick(stepPhysics(current,0.15*timeWarp,latestPitch.toDouble()))
        }
    }
    val altitude=sim.pos.mag()-EARTH_RADIUS_KM
    val speed=sim.vel.mag()
    val orbit=orbitalMetrics(sim.pos,sim.vel)
    val activeParts=activeStageParts(sim.parts)
    val activeThrust=activeParts.sumOf{it.thrust}
    val twr=if(sim.mass>0)activeThrust/(sim.mass*G0)else 0.0
    val mission=when{
        sim.docked->"DOCKED"
        sim.landed->"RECOVERY"
        altitude>=120&&orbit.periapsisAltitudeKm>=120->"STABLE ORBIT"
        altitude>=80->"SUBORBITAL"
        altitude>30->"ASCENT"
        else->"LAUNCH"
    }
    val rangeMeters=sim.targetPos?.let{(it-sim.pos).mag()*1000.0}
    val relativeSpeed=if(sim.targetVel!=null)(sim.targetVel-sim.vel).mag()*1000.0 else null
    Column(Modifier.fillMaxSize().background(Bg).padding(horizontal=12.dp,vertical=8.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){
            Text("‹",fontSize=36.sp,modifier=Modifier.clickable{onBack()}.padding(horizontal=6.dp),color=Ink)
            Column(Modifier.weight(1f).padding(start=4.dp)){
                Text(if(sim.docking)"DOCKING RANGE" else "MISSION · $mission",fontWeight=FontWeight.Black,fontSize=16.sp,lineHeight=20.sp)
                Text("STAGE ${sim.stage} · ${sim.guidance.name.replace('_',' ')}",color=Muted,fontSize=11.sp,lineHeight=14.sp)
            }
            IconButton(onClick=onMap,modifier=Modifier.size(44.dp)){Text("◎",fontSize=24.sp,color=Cyan)}
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(5.dp),modifier=Modifier.fillMaxWidth()){
            Metric("ALT","%.0f km".format(altitude.coerceAtLeast(0.0)),Modifier.weight(1f))
            Metric("VELOCITY",if(speed<1.0)"%.0f m/s".format(speed*1000.0) else "%.2f km/s".format(speed),Modifier.weight(1f))
            Metric("APO",if(orbit.escape)"ESCAPE" else "%.0f km".format(orbit.apoapsisAltitudeKm),Modifier.weight(1f))
            Metric("PERI",if(orbit.periapsisAltitudeKm<0)"IMPACT" else "%.0f km".format(orbit.periapsisAltitudeKm),Modifier.weight(1f))
        }
        Spacer(Modifier.height(5.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(5.dp),modifier=Modifier.fillMaxWidth()){
            Metric("TWR","%.2f".format(twr),Modifier.weight(1f))
            Metric("MAX Q","%.0f kPa".format(sim.maxDynamicPressureKpa),Modifier.weight(1f))
            Metric("HEAT","%.0f%%".format((sim.heat*100).coerceIn(0.0,150.0)),Modifier.weight(1f))
            Metric("FUEL","%.1f t".format(sim.fuel),Modifier.weight(1f))
        }
        Spacer(Modifier.height(6.dp))
        Surface(color=Panel,shape=RoundedCornerShape(20.dp),modifier=Modifier.fillMaxWidth().weight(1f)){
            Canvas(Modifier.fillMaxSize()){
                val small=min(size.width,size.height)
                val center=Offset(size.width*.5f,size.height*.49f)
                val altitudeLocal=(sim.pos.mag()-EARTH_RADIUS_KM).coerceAtLeast(0.0)
                val scale=(small/(max(35.0,altitudeLocal+35.0)*2.7)).toFloat().coerceIn(.10f,8f)
                drawRect(Brush.verticalGradient(listOf(Color(0xFF050916),Color(0xFF0C1931),Color(0xFF05060B))),size=Size(size.width,size.height))
                // Custom starfield with a subtle blue nebula band.
                drawOval(Brush.radialGradient(listOf(Color(0x223E79D8),Color.Transparent),Offset(size.width*.72f,size.height*.22f),small*.72f),Offset(size.width*.06f,size.height*.02f),Size(size.width*.95f,size.height*.70f))
                repeat(88){i->
                    val sx=((i*83+17)%997)/997f*size.width
                    val sy=((i*47+29)%991)/991f*size.height
                    val radius=if(i%11==0)2f else if(i%3==0)1.3f else .75f
                    drawCircle(if(i%9==0)Color(0xFF93E2FF) else Color.White,radius,Offset(sx,sy),alpha=if(i%5==0).82f else .33f)
                }
                // Camera follows the spacecraft; altitude and the apparent Earth size change smoothly.
                val radial=sim.pos.normalized()
                val earthR=(small*.36f/(1.0+altitudeLocal/1800.0)).toFloat().coerceAtLeast(18f)
                val altitudePixels=(ln(1.0+altitudeLocal/8.0)*26.0).toFloat().coerceAtMost(size.height*.54f)
                val earthCenter=Offset(
                    center.x-radial.x.toFloat()*(earthR+altitudePixels),
                    center.y+radial.y.toFloat()*(earthR+altitudePixels)
                )
                drawCircle(Brush.radialGradient(listOf(Color(0x6654BFFF),Color(0x2254BFFF),Color.Transparent),earthCenter,earthR*1.55f),earthR*1.55f,earthCenter)
                drawCircle(Brush.radialGradient(listOf(Color(0xFF459CD5),Color(0xFF1D568D),Color(0xFF071326)),Offset(earthCenter.x-earthR*.32f,earthCenter.y-earthR*.36f),earthR*1.65f),earthR,earthCenter)
                val continentA=Path().apply{
                    moveTo(earthCenter.x-earthR*.74f,earthCenter.y-earthR*.20f)
                    lineTo(earthCenter.x-earthR*.48f,earthCenter.y-earthR*.43f)
                    lineTo(earthCenter.x-earthR*.20f,earthCenter.y-earthR*.32f)
                    lineTo(earthCenter.x-earthR*.09f,earthCenter.y-earthR*.09f)
                    lineTo(earthCenter.x-earthR*.29f,earthCenter.y+earthR*.12f)
                    lineTo(earthCenter.x-earthR*.39f,earthCenter.y+earthR*.43f)
                    lineTo(earthCenter.x-earthR*.65f,earthCenter.y+earthR*.33f)
                    close()
                }
                drawPath(continentA,Brush.linearGradient(listOf(Color(0xFF89C984),Color(0xFF2C6954)),Offset(earthCenter.x-earthR,earthCenter.y-earthR),Offset(earthCenter.x,earthCenter.y+earthR)))
                val continentB=Path().apply{
                    moveTo(earthCenter.x+earthR*.08f,earthCenter.y-earthR*.43f)
                    lineTo(earthCenter.x+earthR*.48f,earthCenter.y-earthR*.30f)
                    lineTo(earthCenter.x+earthR*.66f,earthCenter.y-earthR*.02f)
                    lineTo(earthCenter.x+earthR*.42f,earthCenter.y+earthR*.18f)
                    lineTo(earthCenter.x+earthR*.22f,earthCenter.y+earthR*.44f)
                    lineTo(earthCenter.x+earthR*.07f,earthCenter.y+earthR*.13f)
                    close()
                }
                drawPath(continentB,Brush.linearGradient(listOf(Color(0xFF8CC681),Color(0xFF3D8058)),Offset(earthCenter.x,earthCenter.y-earthR),Offset(earthCenter.x+earthR,earthCenter.y+earthR)))
                repeat(4){i->
                    val yy=earthCenter.y-earthR*.45f+i*earthR*.27f
                    drawOval(Color(0x99D8F0FF),topLeft=Offset(earthCenter.x-earthR*.67f,yy),size=Size(earthR*1.25f,earthR*.065f),style=Stroke(width=max(1f,earthR*.03f)))
                }
                drawCircle(Color(0xBB59CBFF),earthR,earthCenter,style=Stroke(width=max(1.5f,earthR*.025f)))
                // Flight trail in the camera frame.
                if(sim.trail.size>1){
                    val path=Path()
                    sim.trail.forEachIndexed{idx,p->
                        val q=Offset(center.x+((p.x-sim.pos.x)*scale).toFloat(),center.y-((p.y-sim.pos.y)*scale).toFloat())
                        if(idx==0)path.moveTo(q.x,q.y)else path.lineTo(q.x,q.y)
                    }
                    drawPath(path,color=Color(0x7745D7FF),style=Stroke(width=6f))
                    drawPath(path,color=Cyan,style=Stroke(width=2.4f))
                }
                // The target is projected to a readable reticle; range remains physically measured in the HUD.
                if(sim.docking&&sim.targetPos!=null){
                    val delta=sim.targetPos-sim.pos
                    val u=delta.normalized()
                    val targetScreen=Offset(center.x+u.x.toFloat()*78f,center.y-u.y.toFloat()*78f)
                    drawLine(Color(0x8845D7FF),center,targetScreen,1.5f)
                    drawCircle(Color(0x3345D7FF),17f,targetScreen)
                    drawCircle(Cyan,12f,targetScreen,style=Stroke(width=2f))
                    drawLine(Cyan,Offset(targetScreen.x-17f,targetScreen.y),Offset(targetScreen.x+17f,targetScreen.y),1.5f)
                    drawLine(Cyan,Offset(targetScreen.x,targetScreen.y-17f),Offset(targetScreen.x,targetScreen.y+17f),1.5f)
                    drawCircle(Color.White,3f,targetScreen)
                }
                // Engine plume, body and attitude marker.
                val rp=center
                val fx=cos(sim.angle).toFloat()
                val fy=-sin(sim.angle).toFloat()
                val bx=-fx
                val by=-fy
                val sideX=-fy
                val sideY=fx
                if(sim.fuel>0.01&&sim.throttle>0.01&&!sim.crashed&&!sim.landed&&!sim.docked){
                    val flameLength=14f+sim.throttle.toFloat()*28f
                    val flame=Path().apply{
                        moveTo(rp.x+bx*8f+sideX*3.5f,rp.y+by*8f+sideY*3.5f)
                        lineTo(rp.x+bx*flameLength,rp.y+by*flameLength)
                        lineTo(rp.x+bx*8f-sideX*3.5f,rp.y+by*8f-sideY*3.5f)
                        close()
                    }
                    drawPath(flame,Brush.verticalGradient(listOf(Color(0xFFFFF5C9),Color(0xFFFFA13B),Color(0x55FF4B30)),startY=rp.y-30f,endY=rp.y+36f))
                    drawCircle(Color(0x44FF8A35),10f,Offset(rp.x+bx*10f,rp.y+by*10f))
                    repeat(9){i->
                        val t=i/8f
                        drawCircle(Color(0x44FFC078),1.8f*(1f-t),Offset(rp.x+bx*(12f+i*3.7f)+sin((sim.time+i)*2.4).toFloat()*2f,rp.y+by*(12f+i*3.7f)))
                    }
                }
                val body=Path().apply{
                    moveTo(rp.x+fx*14f,rp.y+fy*14f)
                    lineTo(rp.x-fx*8f+sideX*5.2f,rp.y-fy*8f+sideY*5.2f)
                    lineTo(rp.x-fx*6f,rp.y-fy*6f)
                    lineTo(rp.x-fx*8f-sideX*5.2f,rp.y-fy*8f-sideY*5.2f)
                    close()
                }
                drawPath(body,Brush.linearGradient(listOf(Color.White,Color(0xFF9BACBF),Color(0xFF404F68)),Offset(rp.x-8f,rp.y-8f),Offset(rp.x+8f,rp.y+8f)))
                drawCircle(Color(0xFF45D7FF),2.2f,Offset(rp.x+fx*2.5f,rp.y+fy*2.5f))
                drawCircle(Color.White,2f,rp)
                if(sim.heat>.65){
                    drawRoundRect(Color(0x66FF5369),Offset(2f,2f),Size(size.width-4f,size.height-4f),CornerRadius(18f),style=Stroke(width=4f))
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        Row(verticalAlignment=Alignment.CenterVertically){
            Text("THROTTLE",color=Muted,fontSize=11.sp,fontWeight=FontWeight.Black)
            Spacer(Modifier.width(8.dp))
            Text("${(sim.throttle*100).roundToInt()}%",color=Ink,fontSize=14.sp,fontWeight=FontWeight.Black)
            Spacer(Modifier.weight(1f))
            Text(if(sim.parachuteDeployed)"CHUTE DEPLOYED" else "T+ ${sim.time.roundToInt()} s",color=if(sim.parachuteDeployed)Green else Muted,fontSize=11.sp,fontWeight=FontWeight.Bold)
        }
        Slider(value=sim.throttle.toFloat(),onValueChange={onTick(sim.copy(throttle=it.toDouble()))},valueRange=0f..1f,modifier=Modifier.heightIn(min=36.dp),colors=SliderDefaults.colors(thumbColor=Cyan,activeTrackColor=Cyan))
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp),modifier=Modifier.fillMaxWidth()){
            SmallButton(if(paused)"RESUME" else "PAUSE",{paused=!paused},Modifier.weight(1f))
            SmallButton("STAGE",{onTick(separateStage(sim))},Modifier.weight(1f),enabled=sim.parts.contains(PartType.DECOUPLER))
            SmallButton(if(sim.parachuteDeployed)"CHUTE ✓" else "CHUTE",{
                val currentAlt=sim.pos.mag()-EARTH_RADIUS_KM
                if(PartType.PARACHUTE in activeStageParts(sim.parts)&&currentAlt in 0.0..15.0&&sim.vel.mag()<0.30)onTick(sim.copy(parachuteDeployed=true))
            },Modifier.weight(1f),enabled=PartType.PARACHUTE in activeStageParts(sim.parts)&&altitude in 0.0..15.0&&speed<0.30&&!sim.parachuteDeployed)
            SmallButton("WARP ×$timeWarp",{timeWarp=if(timeWarp==1)5 else if(timeWarp==5)20 else 1},Modifier.weight(1f))
        }
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp),modifier=Modifier.fillMaxWidth()){
            SmallButton("P−",{pitchOffset=(pitchOffset-5f).coerceIn(0f,180f);onTick(sim.copy(guidance=Guidance.MANUAL))},Modifier.weight(1f))
            SmallButton("MAN",{onTick(sim.copy(guidance=Guidance.MANUAL))},Modifier.weight(1f))
            SmallButton("P+",{pitchOffset=(pitchOffset+5f).coerceIn(0f,180f);onTick(sim.copy(guidance=Guidance.MANUAL))},Modifier.weight(1f))
            SmallButton("PRO",{onTick(sim.copy(guidance=Guidance.PROGRADE))},Modifier.weight(1f))
            SmallButton("RET",{onTick(sim.copy(guidance=Guidance.RETROGRADE))},Modifier.weight(1f))
        }
        if(sim.docking&&rangeMeters!=null&&relativeSpeed!=null){
            Surface(color=Panel2,shape=RoundedCornerShape(14.dp),modifier=Modifier.fillMaxWidth().padding(top=4.dp)){
                Column(Modifier.padding(horizontal=10.dp,vertical=7.dp)){
                    Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.fillMaxWidth()){
                        Column(Modifier.weight(1f)){
                            Text("TARGET RANGE",fontSize=10.sp,color=Muted,fontWeight=FontWeight.Bold)
                            Text(if(rangeMeters>=1000)"%.2f km".format(rangeMeters/1000.0) else "%.1f m".format(rangeMeters),fontSize=15.sp,fontWeight=FontWeight.Black)
                        }
                        Column(Modifier.weight(1f)){
                            Text("RELATIVE SPEED",fontSize=10.sp,color=Muted,fontWeight=FontWeight.Bold)
                            Text("%.2f m/s".format(relativeSpeed),fontSize=15.sp,fontWeight=FontWeight.Black,color=if(relativeSpeed<0.5)Green else Orange)
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    Row(horizontalArrangement=Arrangement.spacedBy(6.dp),modifier=Modifier.fillMaxWidth()){
                        SmallButton("APPROACH", {onTick(applyDockingApproach(sim))},Modifier.weight(1f),enabled=!sim.docked)
                        SmallButton("MATCH V", {onTick(matchDockingVelocity(sim))},Modifier.weight(1f),enabled=!sim.docked)
                        SmallButton("DOCK", {val docked=attemptDock(sim);onTick(docked);if(docked.docked)onMission("dock")},Modifier.weight(1f),enabled=!sim.docked&&rangeMeters<=5.0&&relativeSpeed<=0.5)
                    }
                }
            }
        }
        if(sim.crashed){
            Surface(color=Color(0xEE341321),shape=RoundedCornerShape(14.dp),modifier=Modifier.fillMaxWidth().padding(top=4.dp)){Text("VEHICLE LOST · REVIEW TWR, Q AND RE-ENTRY",color=Red,fontWeight=FontWeight.Black,fontSize=12.sp,modifier=Modifier.padding(10.dp))}
        }else if(sim.landed){
            Surface(color=Color(0xEE0D2A1F),shape=RoundedCornerShape(14.dp),modifier=Modifier.fillMaxWidth().padding(top=4.dp)){Text(if(sim.maxAltitudeKm>=80)"SAFE RECOVERY · CLAIM FIRST LIGHT" else "SAFE LANDING · FLIGHT RECORDED",color=Green,fontWeight=FontWeight.Black,fontSize=12.sp,modifier=Modifier.padding(10.dp))}
        }else if(sim.docked){
            Surface(color=Color(0xEE0D2A1F),shape=RoundedCornerShape(14.dp),modifier=Modifier.fillMaxWidth().padding(top=4.dp)){Text("DOCKING SUCCESSFUL · RELATIVE MOTION MATCHED",color=Green,fontWeight=FontWeight.Black,fontSize=12.sp,modifier=Modifier.padding(10.dp))}
        }else if(orbit.periapsisAltitudeKm<0&&altitude>80){
            Text("WARNING · IMPACT TRAJECTORY — RAISE PERIAPSIS",fontSize=11.sp,color=Red,fontWeight=FontWeight.Bold)
        }
    }
}

@Composable
private fun SmallButton(text:String,onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true){
    Button(onClick=onClick,enabled=enabled,modifier=modifier.heightIn(min=44.dp),shape=RoundedCornerShape(11.dp),contentPadding=PaddingValues(horizontal=5.dp,vertical=7.dp),colors=ButtonDefaults.buttonColors(containerColor=Panel2,disabledContainerColor=Color(0xFF10131D))){Text(text,fontSize=10.sp,lineHeight=12.sp,fontWeight=FontWeight.Black)}
}

@Composable
private fun MapScreen(sim:SimState,onBack:()->Unit){
    Shell("Navigation Map","Earth-centred trajectories · logarithmic system map",onBack){
        Surface(color=Panel,shape=RoundedCornerShape(22.dp),modifier=Modifier.fillMaxWidth().weight(1f)){
            Canvas(Modifier.fillMaxSize().padding(10.dp)){
                val c=Offset(size.width/2,size.height/2)
                val mapR=min(size.width,size.height)*.44f
                drawRect(Brush.verticalGradient(listOf(Color(0xFF060A17),Color(0xFF0A1427))),size=Size(size.width,size.height))
                repeat(60){i->drawCircle(Color.White,if(i%7==0)1.5f else .7f,Offset(((i*71)%997)/997f*size.width,((i*37)%991)/991f*size.height),alpha=.45f)}
                fun plotRadius(radiusKm:Double):Float{
                    val earthR=EARTH_RADIUS_KM
                    return if(radiusKm<=earthR*4.0)(15.0+(radiusKm-earthR).coerceAtLeast(0.0)/earthR*12.0).toFloat()
                    else (27.0+ln(radiusKm/(earthR*4.0))*25.0).toFloat().coerceAtMost(mapR)
                }
                listOf(1.03,1.10,1.20,1.5,2.0,3.0).forEach{factor->
                    drawCircle(Color(0x332F4B71),plotRadius(EARTH_RADIUS_KM*factor),c,style=Stroke(width=1f))
                }
                val earthRadius=15f
                drawCircle(Brush.radialGradient(listOf(Color(0xFF56B6E9),Color(0xFF18528A),Color(0xFF081529)),Offset(c.x-4f,c.y-5f),earthRadius*1.8f),earthRadius,c)
                drawCircle(Color(0x994BC3FF),earthRadius,c,style=Stroke(width=1.5f))
                val moonAngle=2.0*PI*sim.time/(27.321661*86400.0)
                val moon=V2(cos(moonAngle)*MOON_ORBIT_KM,sin(moonAngle)*MOON_ORBIT_KM)
                val moonR=plotRadius(MOON_ORBIT_KM)
                val moonSpot=Offset(c.x+cos(moonAngle).toFloat()*moonR,c.y-sin(moonAngle).toFloat()*moonR)
                drawCircle(Color(0x339EA8BD),10f,moonSpot)
                drawCircle(Brush.radialGradient(listOf(Color(0xFFE6E8EE),Color(0xFF858C9D),Color(0xFF3D4555)),Offset(moonSpot.x-2f,moonSpot.y-2f),12f),6f,moonSpot)
                val marsR=plotRadius(227_940_000.0)
                drawCircle(Color(0x558A4A3C),8f,Offset(c.x+marsR,c.y),style=Stroke(width=2f))
                // Actual path points, compressed logarithmically so LEO remains visible beside lunar distance.
                if(sim.trail.size>1){
                    val path=Path()
                    sim.trail.takeLast(500).forEachIndexed{idx,p->
                        val radius=p.mag().coerceAtLeast(1.0)
                        val angle=atan2(p.y,p.x)
                        val rp=plotRadius(radius)
                        val q=Offset(c.x+cos(angle).toFloat()*rp,c.y-sin(angle).toFloat()*rp)
                        if(idx==0)path.moveTo(q.x,q.y)else path.lineTo(q.x,q.y)
                    }
                    drawPath(path,color=Color(0x5545D7FF),style=Stroke(width=5f))
                    drawPath(path,color=Cyan,style=Stroke(width=2f))
                }
                val currentAngle=atan2(sim.pos.y,sim.pos.x)
                val currentR=plotRadius(sim.pos.mag())
                drawCircle(Color.White,3.5f,Offset(c.x+cos(currentAngle).toFloat()*currentR,c.y-sin(currentAngle).toFloat()*currentR))
                if(sim.targetPos!=null&&sim.docking){
                    val targetAngle=atan2(sim.targetPos.y,sim.targetPos.x)
                    val targetR=plotRadius(sim.targetPos.mag())
                    drawCircle(Cyan,5f,Offset(c.x+cos(targetAngle).toFloat()*targetR,c.y-sin(targetAngle).toFloat()*targetR),style=Stroke(width=2f))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Surface(color=Panel,shape=RoundedCornerShape(18.dp)){
            Column(Modifier.padding(14.dp)){
                Text("NAVIGATION COMPUTER",fontWeight=FontWeight.Black,fontSize=12.sp,color=Muted)
                Text("Earth · Moon · Mars",fontWeight=FontWeight.Bold,fontSize=18.sp,modifier=Modifier.padding(top=3.dp))
                Text("Distances in the system view are compressed logarithmically; telemetry and orbit calculations use real kilometre scales.",color=Muted,fontSize=12.sp,lineHeight=17.sp,modifier=Modifier.padding(top=4.dp))
            }
        }
    }
}
private fun worldToScreen(p:V2,rocket:V2,center:Offset,scale:Float):Offset{
    return Offset(center.x+((p.x-rocket.x)*scale).toFloat(),center.y-((p.y-rocket.y)*scale).toFloat())
}
