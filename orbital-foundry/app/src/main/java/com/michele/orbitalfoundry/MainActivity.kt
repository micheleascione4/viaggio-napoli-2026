package com.michele.orbitalfoundry

import android.os.Bundle
import android.content.Context
import android.graphics.BitmapFactory
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.zIndex
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.LocalContext
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
    PartType.TANK,PartType.TANK,PartType.ENGINE,PartType.FIN
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
        val savedParts=preferences.getString("rocket_parts",null)
        val restoredParts=savedParts?.split(",")?.mapNotNull{token->
            runCatching{PartType.valueOf(token)}.getOrNull()
        }.orEmpty()
        val previousDefaultRocket=listOf(
            PartType.NOSE,PartType.CAPSULE,PartType.HEATSHIELD,PartType.PARACHUTE,
            PartType.TANK,PartType.TANK,PartType.VACUUM_ENGINE,PartType.DECOUPLER,
            PartType.TANK,PartType.TANK,PartType.VACUUM_ENGINE,PartType.DECOUPLER,
            PartType.TANK,PartType.TANK,PartType.VACUUM_ENGINE,PartType.DECOUPLER,
            PartType.TANK,PartType.TANK,PartType.ENGINE,PartType.ENGINE,PartType.ENGINE,PartType.ENGINE,PartType.FIN
        )
        val initialRocket=if(restoredParts.isEmpty()||restoredParts==previousDefaultRocket)starterRocket()else Rocket(restoredParts)
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

private fun loadPlanetTexture(context:Context,name:String):ImageBitmap? =
    runCatching{
        context.assets.open("textures/$name").use{stream->
            BitmapFactory.decodeStream(stream)?.asImageBitmap()
        }
    }.getOrNull()

private fun DrawScope.drawTexturedPlanet(
    texture:ImageBitmap?,center:Offset,radius:Float,
    fallback:List<Color>
){
    if(radius<=1f)return
    drawCircle(Brush.radialGradient(fallback,Offset(center.x-radius*.32f,center.y-radius*.32f),radius*1.9f),radius,center)
    if(texture!=null){
        val planetPath=Path().apply{addOval(androidx.compose.ui.geometry.Rect(center.x-radius,center.y-radius,center.x+radius,center.y+radius))}
        val dstOffset=IntOffset((center.x-radius).roundToInt(),(center.y-radius).roundToInt())
        val dstSize=IntSize((radius*2f).roundToInt().coerceAtLeast(1),(radius*2f).roundToInt().coerceAtLeast(1))
        clipPath(planetPath){
            drawImage(
                image=texture,
                srcOffset=IntOffset(texture.width/4,0),
                srcSize=IntSize((texture.width/2).coerceAtLeast(1),texture.height),
                dstOffset=dstOffset,
                dstSize=dstSize,
                filterQuality=FilterQuality.High
            )
            drawCircle(
                Brush.radialGradient(
                    colors=listOf(Color.Transparent,Color(0x66030A20)),
                    center=Offset(center.x+radius*.52f,center.y+radius*.12f),
                    radius=radius*1.18f
                ),
                radius,
                center
            )
        }
    }
    drawCircle(Color(0x664BC3FF),radius,center,style=Stroke(width=max(1.2f,radius*.018f)))
}

@Composable
fun OrbitalFoundryApp(
    initialRocket:Rocket,
    initialMissions:Set<String>,
    initialCareer:CareerState,
    onTutorialComplete:()->Unit,
    onSaveRocket:(Rocket)->Unit,
    onSaveMissions:(Set<String>)->Unit,
    onSaveCareer:(CareerState)->Unit
){
    val context=LocalContext.current
    val earthTexture=remember{loadPlanetTexture(context,"earth.png")}
    val marsTexture=remember{loadPlanetTexture(context,"mars.jpg")}
    MaterialTheme(colorScheme=darkColorScheme(background=Bg,surface=Panel,primary=Color(0xFF3479B5),secondary=Cyan,onBackground=Ink,onSurface=Ink)){
        var screen by rememberSaveable{mutableStateOf(Screen.HOME)}
        var rocket by remember{mutableStateOf(initialRocket)}
        var sim by remember{mutableStateOf<SimState?>(null)}
        var missions by remember{mutableStateOf(initialMissions)}
        var career by remember{mutableStateOf(initialCareer)}
        var sandboxMode by rememberSaveable{mutableStateOf(false)}
        LaunchedEffect(rocket){onSaveRocket(rocket)}
        LaunchedEffect(missions){onSaveMissions(missions)}
        LaunchedEffect(career){onSaveCareer(career)}
        when(screen){
            Screen.HOME->HomeScreen(rocket,missions,career,
                {sandboxMode=false;screen=Screen.BUILD},
                {if(rocket.hasEngine&&rocket.hasCapsule){sim=launchState(rocket);screen=Screen.FLIGHT}},
                {screen=Screen.MISSIONS},
                {screen=Screen.TUTORIAL},
                {sandboxMode=true;screen=Screen.BUILD},
                {if(rocket.hasDockingPort&&rocket.hasRcs){sim=beginDockingPractice(rocket);screen=Screen.FLIGHT}}
            )
            Screen.BUILD->BuilderScreen(rocket,career.unlocked,sandboxMode,
                {p->rocket=addPartInUsefulPosition(rocket,p)},
                {part,index->rocket=insertPartAt(rocket,part,index)},
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
            Screen.FLIGHT->{val s=sim;if(s!=null)FlightScreen(s,earthTexture,marsTexture,
                {next->sim=next;missions=missions+missionCompletions(next)},
                {screen=Screen.MAP},{screen=Screen.HOME},
                {name->missions=missions+name}
            )}
            Screen.MAP->{val s=sim;if(s!=null)MapScreen(s,earthTexture,marsTexture,{screen=Screen.FLIGHT})}
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

private fun insertPartAt(rocket:Rocket,part:PartType,index:Int):Rocket{
    val parts=rocket.parts.toMutableList()
    parts.add(index.coerceIn(0,parts.size),part)
    return rocket.copy(parts=parts)
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
    onBuild:()->Unit,onLaunch:()->Unit,onMissions:()->Unit,onTutorial:()->Unit,
    onSandbox:()->Unit,onDocking:()->Unit
){
    val page=Color(0xFFE8F0F7)
    val card=Color(0xFFFAFCFE)
    val dark=Color(0xFF1C3044)
    val secondary=Color(0xFF63778B)
    val blue=Color(0xFF3479B5)
    Column(Modifier.fillMaxSize().background(page).verticalScroll(rememberScrollState()).padding(horizontal=15.dp,vertical=10.dp)){
        Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.fillMaxWidth()){
            Column(Modifier.weight(1f)){
                Text("ORBITAL FOUNDRY",fontSize=23.sp,fontWeight=FontWeight.Black,lineHeight=27.sp,letterSpacing=(-.5).sp,color=dark)
                Text("BUILD  ·  LAUNCH  ·  EXPLORE",color=blue,fontSize=10.sp,fontWeight=FontWeight.Bold,letterSpacing=1.2.sp)
            }
            TextButton(onClick=onTutorial,contentPadding=PaddingValues(horizontal=10.dp,vertical=6.dp),colors=ButtonDefaults.textButtonColors(contentColor=dark)){
                Text("GUIDE",fontSize=11.sp,fontWeight=FontWeight.Black)
            }
        }
        Spacer(Modifier.height(9.dp))
        Box(Modifier.fillMaxWidth().height(218.dp).background(Color(0xFF4C83B8),RoundedCornerShape(10.dp))){
            RocketStackPreview(rocket,Modifier.fillMaxSize())
            Surface(color=Color(0xE8EDF4FA),shape=RoundedCornerShape(bottomEnd=8.dp),modifier=Modifier.align(Alignment.TopStart)){
                Column(Modifier.padding(horizontal=10.dp,vertical=7.dp)){
                    Text("ACTIVE VEHICLE",fontSize=9.sp,color=blue,fontWeight=FontWeight.Black,letterSpacing=.9.sp)
                    Text(if(rocket.hasEngine&&rocket.hasCapsule)"READY TO LAUNCH" else "INCOMPLETE ROCKET",fontSize=13.sp,fontWeight=FontWeight.Black,color=dark)
                }
            }
            Row(horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.CenterVertically,modifier=Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color(0xEAF4F8FC)).padding(horizontal=7.dp,vertical=7.dp)){
                Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.weight(1f)){
                    Text("MASS",fontSize=9.sp,color=secondary,fontWeight=FontWeight.Bold)
                    Text("%.1f t".format(rocket.dryMass+rocket.fuel),fontSize=12.sp,fontWeight=FontWeight.Black,color=dark)
                }
                Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.weight(1f)){
                    Text("FUEL",fontSize=9.sp,color=secondary,fontWeight=FontWeight.Bold)
                    Text("%.0f t".format(rocket.fuel),fontSize=12.sp,fontWeight=FontWeight.Black,color=dark)
                }
                Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.weight(1f)){
                    Text("ΔV",fontSize=9.sp,color=secondary,fontWeight=FontWeight.Bold)
                    Text("%.2f km/s".format(rocket.deltaV),fontSize=12.sp,fontWeight=FontWeight.Black,color=blue)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Button(onClick=onBuild,modifier=Modifier.fillMaxWidth().height(49.dp),shape=RoundedCornerShape(8.dp),contentPadding=PaddingValues(10.dp),colors=ButtonDefaults.buttonColors(containerColor=blue,contentColor=Color.White)){
            Text("BUILD ROCKET",fontWeight=FontWeight.Black,letterSpacing=.7.sp)
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth()){
            OutlinedButton(onClick=onSandbox,modifier=Modifier.weight(1f).height(46.dp),shape=RoundedCornerShape(8.dp),colors=ButtonDefaults.outlinedButtonColors(contentColor=dark)){
                Text("SANDBOX",fontWeight=FontWeight.Black,fontSize=12.sp)
            }
            OutlinedButton(onClick=onLaunch,enabled=rocket.hasEngine&&rocket.hasCapsule,modifier=Modifier.weight(1f).height(46.dp),shape=RoundedCornerShape(8.dp),colors=ButtonDefaults.outlinedButtonColors(contentColor=blue)){
                Text("QUICK LAUNCH",fontWeight=FontWeight.Black,fontSize=12.sp)
            }
        }
        Spacer(Modifier.height(9.dp))
        Surface(color=card,shape=RoundedCornerShape(10.dp),modifier=Modifier.fillMaxWidth()){
            Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.padding(horizontal=11.dp,vertical=10.dp)){
                Column(Modifier.weight(1f)){
                    Text("CAREER",fontSize=10.sp,color=secondary,fontWeight=FontWeight.Black,letterSpacing=1.sp)
                    Text("€${career.funds/1000}K   ·   ${career.science} SCI",fontSize=13.sp,fontWeight=FontWeight.Bold,color=dark)
                    Text("${career.unlocked.size} technologies unlocked",fontSize=10.sp,color=secondary)
                }
                Button(onClick=onMissions,shape=RoundedCornerShape(7.dp),contentPadding=PaddingValues(horizontal=10.dp,vertical=8.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFFD6E6F4),contentColor=dark)){
                    Text("TECH TREE  →",fontSize=10.sp,fontWeight=FontWeight.Black)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth()){
            OutlinedButton(onClick=onMissions,modifier=Modifier.weight(1f).height(42.dp),shape=RoundedCornerShape(8.dp),colors=ButtonDefaults.outlinedButtonColors(contentColor=dark)){
                Text("MISSIONS  (${missions.size})",fontSize=10.sp,fontWeight=FontWeight.Bold)
            }
            OutlinedButton(onClick=onDocking,enabled=rocket.hasDockingPort&&rocket.hasRcs,modifier=Modifier.weight(1f).height(42.dp),shape=RoundedCornerShape(8.dp),colors=ButtonDefaults.outlinedButtonColors(contentColor=dark)){
                Text("DOCKING RANGE",fontSize=10.sp,fontWeight=FontWeight.Bold)
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

private fun stackPreviewScale(height:Float,count:Int,density:Float):Float{
    if(count<=0)return 1f
    // Canvas coordinates are pixels: scale the parts from dp so they remain legible on high-density phones.
    val available=(height-110f*density).coerceAtLeast(60f*density)
    return min(1f,available/(count*94f*density)).coerceIn(.12f,1f)
}

private fun stackInsertionIndex(y:Float,height:Float,count:Int,density:Float):Int{
    if(count<=0)return 0
    val scale=stackPreviewScale(height,count,density)
    val partH=88f*density*scale
    val step=94f*density*scale
    val bottomCenter=height-82f*density*scale-partH/2f
    val topCenter=bottomCenter-(count-1)*step
    if(y<topCenter-partH/2f)return 0
    if(y>bottomCenter+partH/2f)return count
    val rowFromTop=((y-topCenter)/step).roundToInt().coerceIn(0,count-1)
    val center=topCenter+rowFromTop*step
    return (rowFromTop+if(y>center)1 else 0).coerceIn(0,count)
}

@Composable
private fun RocketStackPreview(
    rocket:Rocket,
    modifier:Modifier=Modifier,
    onReorder:((Int,Int)->Unit)?=null
){
    val reorderModifier=if(onReorder!=null) Modifier.pointerInput(rocket.parts,onReorder){
        var from=-1
        var to=-1
        detectDragGestures(
            onDragStart={point->
                val count=rocket.parts.size
                if(count>0){
                    val density=androidx.compose.ui.platform.LocalDensity.current.density
                    val scale=stackPreviewScale(size.height.toFloat(),count,density)
                    val partH=88f*density*scale
                    val bottomCenter=size.height-82f*density*scale-partH/2f
                    val row=((bottomCenter-point.y)/(76f*scale)).roundToInt().coerceIn(0,count-1)
                    from=count-1-row
                    to=from
                }
            },
            onDrag={change,_->
                change.consume()
                val count=rocket.parts.size
                if(count>0&&from>=0){
                    val density=androidx.compose.ui.platform.LocalDensity.current.density
                    val scale=stackPreviewScale(size.height.toFloat(),count,density)
                    val partH=88f*density*scale
                    val bottomCenter=size.height-82f*density*scale-partH/2f
                    val row=((bottomCenter-change.position.y)/(94f*density*scale)).roundToInt().coerceIn(0,count-1)
                    to=count-1-row
                }
            },
            onDragEnd={
                val source=from
                val destination=to
                from=-1
                to=-1
                if(source>=0&&destination>=0&&source!=destination)onReorder.invoke(source,destination-source)
            },
            onDragCancel={from=-1;to=-1}
        )
    }else Modifier
    Canvas(modifier.fillMaxSize().then(reorderModifier)){
        drawRect(Brush.verticalGradient(listOf(Color(0xFF5B91C7),Color(0xFF4C83B8),Color(0xFF3D70A0))),size=Size(size.width,size.height))
        val grid=42f
        for(x in 0..(size.width/grid).toInt())drawLine(Color(0x337CB7E7),Offset(x*grid,0f),Offset(x*grid,size.height),1f)
        for(y in 0..(size.height/grid).toInt())drawLine(Color(0x337CB7E7),Offset(0f,y*grid),Offset(size.width,y*grid),1f)
        drawLine(Color(0x2295C8F1),Offset(size.width*.5f,0f),Offset(size.width*.5f,size.height),1f)
        val count=rocket.parts.size
        val density=androidx.compose.ui.platform.LocalDensity.current.density
        val scale=stackPreviewScale(size.height,count,density)
        val partH=88f*density*scale
        val partGap=6f*density*scale
        val partW=min(82f*density,size.width*.42f)*scale
        val left=(size.width-partW)*.5f
        var y=size.height-82f*density*scale-partH
        rocket.parts.asReversed().forEach{part->
            drawRocketComponent(part,left,y,partW,partH)
            y-=partH+partGap
        }
        val platformY=size.height-54f*scale
        drawRoundRect(Color(0xFF31445C),Offset(size.width*.27f,platformY),Size(size.width*.46f,8f*scale),CornerRadius(3f*scale))
        drawLine(Color(0xFF98C8EA),Offset(size.width*.18f,platformY+9f*scale),Offset(size.width*.82f,platformY+9f*scale),1.5f*scale)
        if(rocket.parts.isEmpty()){
            val c=Offset(size.width*.5f,size.height*.42f)
            drawCircle(Color(0x99FFFFFF),18f,c,style=Stroke(width=2f))
            drawLine(Color(0x66FFFFFF),Offset(c.x-26f,c.y),Offset(c.x+26f,c.y),1f)
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
private fun BuilderScreen(
    rocket:Rocket,unlockedTech:Set<String>,sandboxMode:Boolean,
    onAdd:(PartType)->Unit,onDrop:(PartType,Int)->Unit,onAddStage:()->Unit,
    onRemove:(Int)->Unit,onMove:(Int,Int)->Unit,onClear:()->Unit,onBack:()->Unit,onLaunch:()->Unit
){
    var showStack by rememberSaveable{mutableStateOf(false)}
    val effectiveUnlocked=if(sandboxMode)CareerDatabase.tech.map{it.id}.toSet() else unlockedTech
    var rootBounds by remember{mutableStateOf<Rect?>(null)}
    var viewportBounds by remember{mutableStateOf<Rect?>(null)}
    var draggingPart by remember{mutableStateOf<PartType?>(null)}
    var dragPosition by remember{mutableStateOf<Offset?>(null)}
    val density=androidx.compose.ui.platform.LocalDensity.current.density
    val sideMargin=with(androidx.compose.ui.platform.LocalDensity.current){78.dp.toPx()}
    val rightMargin=with(androidx.compose.ui.platform.LocalDensity.current){78.dp.toPx()}
    Box(Modifier.fillMaxSize().background(Color(0xFFE4ECF4)).onGloballyPositioned{rootBounds=it.boundsInWindow()}){
        Column(Modifier.fillMaxSize()){
            Row(Modifier.fillMaxWidth().heightIn(min=49.dp).background(Color(0xFF345F85)).padding(horizontal=9.dp,vertical=4.dp),verticalAlignment=Alignment.CenterVertically){
                Surface(color=Color(0x334A83B5),shape=RoundedCornerShape(6.dp),modifier=Modifier.clickable{onBack()}){
                    Text("‹",fontSize=30.sp,color=Color.White,modifier=Modifier.padding(horizontal=9.dp,vertical=1.dp))
                }
                Column(Modifier.weight(1f).padding(start=8.dp)){
                    Text("ROCKET BUILDER",fontSize=14.sp,fontWeight=FontWeight.Black,lineHeight=17.sp,color=Color.White)
                    Text(if(sandboxMode)"SANDBOX · ALL PARTS FREE" else "CAREER · BUILD WITH UNLOCKED PARTS",fontSize=9.sp,color=Color(0xFFD2E7F8),lineHeight=12.sp)
                }
                Button(onClick=onLaunch,enabled=rocket.hasEngine&&rocket.hasCapsule,modifier=Modifier.height(39.dp),shape=RoundedCornerShape(7.dp),contentPadding=PaddingValues(horizontal=11.dp,vertical=5.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFFE7F1F9),contentColor=Color(0xFF183650))){
                    Text("LAUNCH  ▶",fontSize=10.sp,fontWeight=FontWeight.Black)
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth().background(Color(0xFF4D80B4)).onGloballyPositioned{viewportBounds=it.boundsInWindow()}){
                RocketStackPreview(rocket,Modifier.fillMaxSize(),onReorder=onMove)
                Column(Modifier.align(Alignment.CenterStart).width(78.dp).fillMaxHeight().background(Color(0xEAF0F5FA)).padding(horizontal=4.dp,vertical=5.dp)){
                    Text("PARTS",fontSize=9.sp,fontWeight=FontWeight.Black,color=Color(0xFF356B9D),modifier=Modifier.align(Alignment.CenterHorizontally).padding(bottom=5.dp))
                    LazyColumn(verticalArrangement=Arrangement.spacedBy(4.dp),modifier=Modifier.fillMaxSize()){
                        items(PartType.entries.toList(),key={it.name}){part->
                            val available=isPartUnlocked(part,effectiveUnlocked)
                            var itemCoordinates by remember(part){mutableStateOf<LayoutCoordinates?>(null)}
                            val dragModifier=if(available)Modifier.pointerInput(part,rocket.parts.size){
                                detectDragGestures(
                                    onDragStart={start->
                                        val origin=itemCoordinates?.localToWindow(Offset.Zero)?:Offset.Zero
                                        draggingPart=part
                                        dragPosition=origin+start
                                    },
                                    onDrag={change,delta->
                                        change.consume()
                                        dragPosition=dragPosition?.plus(delta)
                                    },
                                    onDragEnd={
                                        val position=dragPosition
                                        val bounds=viewportBounds
                                        if(position!=null&&bounds!=null&&position.x>bounds.left+sideMargin&&position.x<bounds.right-rightMargin&&position.y>=bounds.top&&position.y<=bounds.bottom){
                                            val localY=(position.y-bounds.top).coerceIn(0f,bounds.height)
                                            onDrop(part,stackInsertionIndex(localY,bounds.height,rocket.parts.size,density))
                                        }
                                        draggingPart=null
                                        dragPosition=null
                                    },
                                    onDragCancel={draggingPart=null;dragPosition=null}
                                )
                            }else Modifier
                            Surface(
                                color=if(available)Color(0xFFF9FCFE) else Color(0xFFD2DDE7),
                                shape=RoundedCornerShape(7.dp),
                                modifier=Modifier.fillMaxWidth().height(65.dp).onGloballyPositioned{itemCoordinates=it}.then(dragModifier)
                                    .clickable(enabled=available){onAdd(part)}
                                    .border(1.dp,if(available)Color(0xFFB8CDE0) else Color(0xFFC4CED7),RoundedCornerShape(7.dp))
                            ){
                                Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center,modifier=Modifier.fillMaxWidth().padding(vertical=1.dp)){
                                    RocketPartThumbnail(part)
                                    Text(if(available)part.title else "LOCKED",fontSize=7.sp,lineHeight=8.sp,maxLines=2,textAlign=androidx.compose.ui.text.style.TextAlign.Center,color=if(available)Color(0xFF263F54) else Color(0xFF7F8994),fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=2.dp))
                                }
                            }
                        }
                    }
                }
                Surface(color=Color(0xEAF0F5FA),shape=RoundedCornerShape(topStart=7.dp,bottomStart=7.dp),modifier=Modifier.align(Alignment.CenterEnd).padding(end=5.dp)){
                    Column(Modifier.padding(horizontal=7.dp,vertical=8.dp),horizontalAlignment=Alignment.End){
                        Text("MASS",fontSize=8.sp,color=Color(0xFF60758A),fontWeight=FontWeight.Black)
                        Text("%.1f t".format(rocket.dryMass+rocket.fuel),fontSize=12.sp,fontWeight=FontWeight.Black,color=Color(0xFF1F354B))
                        Spacer(Modifier.height(4.dp))
                        Text("T / W",fontSize=8.sp,color=Color(0xFF60758A),fontWeight=FontWeight.Black)
                        Text("%.2f".format(analyzeVehicle(rocket).twr),fontSize=12.sp,fontWeight=FontWeight.Black,color=if(analyzeVehicle(rocket).twr>=1.2)Color(0xFF217C57) else Color(0xFFB56B1C))
                        Spacer(Modifier.height(4.dp))
                        Text("ΔV",fontSize=8.sp,color=Color(0xFF60758A),fontWeight=FontWeight.Black)
                        Text("%.1f".format(rocket.deltaV),fontSize=12.sp,fontWeight=FontWeight.Black,color=Color(0xFF246DA5))
                    }
                }
                Text("TOP  ↑",modifier=Modifier.align(Alignment.TopCenter).padding(top=8.dp),fontSize=9.sp,color=Color(0xEFFFFFFF),fontWeight=FontWeight.Black)
                if(rocket.parts.isNotEmpty()){
                    Text("Drag parts here · drag the rocket to reorder",modifier=Modifier.align(Alignment.BottomCenter).padding(bottom=8.dp).background(Color(0x88273F58),RoundedCornerShape(5.dp)).padding(horizontal=7.dp,vertical=4.dp),fontSize=8.sp,color=Color.White)
                }
            }
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalAlignment=Alignment.CenterVertically,modifier=Modifier.fillMaxWidth().background(Color(0xFFE4ECF4)).padding(horizontal=7.dp,vertical=5.dp)){
                Text("${rocket.parts.size} PARTS",color=Color(0xFF526A80),fontSize=9.sp,fontWeight=FontWeight.Black,modifier=Modifier.weight(1f))
                OutlinedButton(onClick={showStack=true},contentPadding=PaddingValues(horizontal=8.dp,vertical=7.dp),shape=RoundedCornerShape(7.dp),colors=ButtonDefaults.outlinedButtonColors(contentColor=Color(0xFF253F56))){Text("STACK",fontWeight=FontWeight.Black,fontSize=9.sp)}
                OutlinedButton(onClick=onAddStage,contentPadding=PaddingValues(horizontal=8.dp,vertical=7.dp),shape=RoundedCornerShape(7.dp),colors=ButtonDefaults.outlinedButtonColors(contentColor=Color(0xFF253F56))){Text("+ STAGE",fontWeight=FontWeight.Black,fontSize=9.sp)}
                OutlinedButton(onClick=onClear,contentPadding=PaddingValues(horizontal=8.dp,vertical=7.dp),shape=RoundedCornerShape(7.dp),colors=ButtonDefaults.outlinedButtonColors(contentColor=Color(0xFF253F56))){Text("RESET",fontWeight=FontWeight.Black,fontSize=9.sp)}
            }
        }
        if(draggingPart!=null&&dragPosition!=null&&rootBounds!=null){
            val point=dragPosition!!
            val root=rootBounds!!
            Box(Modifier.offset{IntOffset((point.x-root.left-38f).roundToInt(),(point.y-root.top-42f).roundToInt())}.zIndex(5f).width(76.dp).height(84.dp).background(Color(0xF7F9FCFE),RoundedCornerShape(9.dp)).border(2.dp,Color(0xFF3479B5),RoundedCornerShape(9.dp)),contentAlignment=Alignment.Center){
                Column(horizontalAlignment=Alignment.CenterHorizontally){
                    RocketPartThumbnail(draggingPart!!)
                    Text(draggingPart!!.title,fontSize=8.sp,lineHeight=9.sp,maxLines=2,color=Color(0xFF1D344A),fontWeight=FontWeight.Bold,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
        if(showStack){
            AlertDialog(
                onDismissRequest={showStack=false},
                title={Text("ROCKET STACK · TOP TO BOTTOM",fontWeight=FontWeight.Black,color=Color(0xFF1D344A))},
                text={
                    if(rocket.parts.isEmpty())Text("The stack is empty. Drag a component from the parts rail.")
                    else LazyColumn(verticalArrangement=Arrangement.spacedBy(5.dp),modifier=Modifier.heightIn(max=390.dp)){
                        itemsIndexed(rocket.parts){index,part->
                            Row(Modifier.fillMaxWidth().background(Color(0xFFE7EFF6),RoundedCornerShape(7.dp)).padding(horizontal=7.dp,vertical=5.dp),verticalAlignment=Alignment.CenterVertically){
                                RocketPartThumbnail(part)
                                Column(Modifier.weight(1f).padding(start=7.dp)){
                                    Text(part.title,fontSize=12.sp,fontWeight=FontWeight.Bold,color=Color(0xFF1D344A),lineHeight=14.sp)
                                    Text("M %.1f t · F %.1f t".format(part.mass,part.fuel),fontSize=10.sp,color=Color(0xFF61768A))
                                }
                                Column(horizontalAlignment=Alignment.CenterHorizontally){
                                    Text("↑",fontSize=17.sp,color=Color(0xFF3479B5),modifier=Modifier.clickable{onMove(index,-1)}.padding(horizontal=6.dp))
                                    Text("↓",fontSize=17.sp,color=Color(0xFF3479B5),modifier=Modifier.clickable{onMove(index,1)}.padding(horizontal=6.dp))
                                }
                                Text("×",fontSize=20.sp,color=Color(0xFFB34758),modifier=Modifier.clickable{onRemove(index)}.padding(horizontal=6.dp))
                            }
                        }
                    }
                },
                confirmButton={TextButton(onClick={showStack=false},colors=ButtonDefaults.textButtonColors(contentColor=Color(0xFF3479B5))){Text("DONE")}},
                containerColor=Color(0xFFF7FAFD)
            )
        }
    }
}
@Composable
private fun TutorialScreen(onFinish:()->Unit,onSkip:()->Unit){
    var page by rememberSaveable{mutableIntStateOf(0)}
    val lessons=listOf(
        Triple("01 / ASSEMBLA","Trascina i componenti dalla barra sinistra al razzo. Trascina i moduli già montati per cambiarne l'ordine.","Tocca un pezzo per aggiungerlo automaticamente."),
        Triple("02 / LANCIA","Premi LAUNCH. Usa i controlli laterali per inclinare il razzo e STAGE per separare uno stadio.","In Sandbox tutti i pezzi sono disponibili senza limiti di tecnologia."),
        Triple("03 / RAGGIUNGI L'ORBITA","Sali, controlla la mappa e inclina gradualmente verso l'orizzonte. Una buona orbita ha periapside e apoapside sopra l'atmosfera.","Non serve leggere tutto prima di giocare: torna qui dal menu quando ti serve.")
    )
    val lesson=lessons[page]
    val dark=Color(0xFF20374D)
    val blue=Color(0xFF3479B5)
    Column(Modifier.fillMaxSize().background(Color(0xFFE8F0F7)).padding(horizontal=16.dp,vertical=12.dp)){
        Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.fillMaxWidth()){
            Column(Modifier.weight(1f)){
                Text("QUICK GUIDE",fontSize=11.sp,color=blue,fontWeight=FontWeight.Black,letterSpacing=1.3.sp)
                Text("Impara giocando",fontSize=25.sp,fontWeight=FontWeight.Black,color=dark)
            }
            Text("${page+1} / ${lessons.size}",fontSize=12.sp,color=Color(0xFF60758A),fontWeight=FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        Surface(color=Color(0xFFFAFCFE),shape=RoundedCornerShape(12.dp),modifier=Modifier.fillMaxWidth().weight(1f)){
            Column(Modifier.padding(15.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                Surface(color=Color(0xFFE4EDF6),shape=RoundedCornerShape(9.dp),modifier=Modifier.fillMaxWidth()){
                    Box(Modifier.height(135.dp).fillMaxWidth(),contentAlignment=Alignment.Center){
                        RocketStackPreview(Rocket(listOf(PartType.NOSE,PartType.CAPSULE,PartType.HEATSHIELD,PartType.TANK,PartType.TANK,PartType.ENGINE)))
                    }
                }
                Text(lesson.first,fontSize=11.sp,color=blue,fontWeight=FontWeight.Black,letterSpacing=.8.sp)
                Text(lesson.second,fontSize=17.sp,lineHeight=23.sp,fontWeight=FontWeight.SemiBold,color=dark)
                Text(lesson.third,fontSize=13.sp,lineHeight=18.sp,color=Color(0xFF63778B))
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement=Arrangement.spacedBy(5.dp),modifier=Modifier.fillMaxWidth()){
                    lessons.indices.forEach{idx->
                        Box(Modifier.weight(1f).height(4.dp).background(if(idx<=page)blue else Color(0xFFD5E1EB),RoundedCornerShape(4.dp)))
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(7.dp),modifier=Modifier.fillMaxWidth()){
            OutlinedButton(onClick=onSkip,modifier=Modifier.weight(1f).height(46.dp),shape=RoundedCornerShape(8.dp),colors=ButtonDefaults.outlinedButtonColors(contentColor=dark)){Text("SALTA",fontWeight=FontWeight.Bold)}
            if(page>0) OutlinedButton(onClick={page--},modifier=Modifier.weight(1f).height(46.dp),shape=RoundedCornerShape(8.dp),colors=ButtonDefaults.outlinedButtonColors(contentColor=dark)){Text("INDIETRO",fontWeight=FontWeight.Bold,fontSize=10.sp)}
            Button(onClick={if(page==lessons.lastIndex)onFinish() else page++},modifier=Modifier.weight(1.4f).height(46.dp),shape=RoundedCornerShape(8.dp),colors=ButtonDefaults.buttonColors(containerColor=blue,contentColor=Color.White)){
                Text(if(page==lessons.lastIndex)"GIOCA" else "AVANTI",fontWeight=FontWeight.Black)
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
private fun FlightScreen(
    sim:SimState,earthTexture:ImageBitmap?,marsTexture:ImageBitmap?,
    onTick:(SimState)->Unit,onMap:()->Unit,onBack:()->Unit,onMission:(String)->Unit
){
    var paused by rememberSaveable{mutableStateOf(false)}
    var pitchOffset by rememberSaveable{mutableFloatStateOf(90f)}
    var timeWarp by rememberSaveable{mutableIntStateOf(1)}
    var showAdvanced by rememberSaveable{mutableStateOf(false)}
    var cameraZoom by rememberSaveable{mutableFloatStateOf(1f)}
    var cameraPan by remember{mutableStateOf(Offset.Zero)}
    val latestSim by rememberUpdatedState(sim)
    val latestPitch by rememberUpdatedState(pitchOffset)

    LaunchedEffect(paused,timeWarp,sim.crashed,sim.landed,sim.docked){
        if(paused||sim.crashed||sim.landed||sim.docked)return@LaunchedEffect
        while(true){
            delay(40L)
            val current=latestSim
            if(current.crashed||current.landed||current.docked)break
            onTick(stepPhysics(current,0.15*timeWarp,latestPitch.toDouble()))
        }
    }

    val altitude=(sim.pos.mag()-EARTH_RADIUS_KM).coerceAtLeast(0.0)
    val speed=sim.vel.mag()
    val orbit=orbitalMetrics(sim.pos,sim.vel)
    val active=activeStageParts(sim.parts)
    val thrust=active.sumOf{it.thrust}
    val twr=if(sim.mass>0)thrust/(sim.mass*G0)else 0.0
    val rangeMeters=sim.targetPos?.let{(it-sim.pos).mag()*1000.0}
    val relativeSpeed=sim.targetVel?.let{(it-sim.vel).mag()*1000.0}
    val mission=when{
        sim.docked->"DOCKED"
        sim.landed->"RECOVERY"
        altitude>=100&&orbit.periapsisAltitudeKm>=100->"ORBIT"
        altitude>=80->"SUBORBITAL"
        altitude>25->"ASCENT"
        else->"LAUNCH"
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF050914))){
        Canvas(
            Modifier.fillMaxSize().pointerInput(Unit){
                detectTransformGestures{_,pan,zoom,_->
                    cameraZoom=(cameraZoom*zoom).coerceIn(0.22f,3.2f)
                    cameraPan=Offset(
                        (cameraPan.x+pan.x).coerceIn(-size.width*.36f,size.width*.36f),
                        (cameraPan.y+pan.y).coerceIn(-size.height*.32f,size.height*.32f)
                    )
                }
            }
        ){
            val small=min(size.width,size.height)
            val center=Offset(size.width*.5f,size.height*.48f)
            val altitudeLocal=(sim.pos.mag()-EARTH_RADIUS_KM).coerceAtLeast(0.0)
            drawRect(Brush.verticalGradient(listOf(Color(0xFF050A17),Color(0xFF10233D),Color(0xFF040711))),size=Size(size.width,size.height))
            drawOval(
                Brush.radialGradient(listOf(Color(0x224A8DDB),Color.Transparent),Offset(size.width*.68f,size.height*.26f),small*.8f),
                Offset(size.width*.08f,size.height*.03f),Size(size.width*.88f,size.height*.72f)
            )
            repeat(105){i->
                val sx=((i*83+17)%997)/997f*size.width
                val sy=((i*47+29)%991)/991f*size.height
                val r=if(i%13==0)2f else if(i%3==0)1.2f else .7f
                drawCircle(if(i%11==0)Color(0xFF93E2FF)else Color.White,r,Offset(sx,sy),alpha=if(i%5==0).78f else .30f)
            }

            // Camera distance adapts from a surface view to a high-orbit view with pinch gestures.
            val radial=sim.pos.normalized()
            val earthR=(small*.36f/(1.0+altitudeLocal/1800.0)*cameraZoom).toFloat().coerceIn(7f,small*.62f)
            val altitudePixels=(ln(1.0+altitudeLocal/8.0)*25.0*cameraZoom).toFloat().coerceAtMost(size.height*.66f)
            val earthCenter=Offset(
                center.x+cameraPan.x-radial.x.toFloat()*(earthR+altitudePixels),
                center.y+cameraPan.y+radial.y.toFloat()*(earthR+altitudePixels)
            )
            drawCircle(
                Brush.radialGradient(listOf(Color(0x6654BFFF),Color(0x2254BFFF),Color.Transparent),earthCenter,earthR*1.55f),
                earthR*1.55f,earthCenter
            )
            drawTexturedPlanet(earthTexture,earthCenter,earthR,listOf(Color(0xFF428BBF),Color(0xFF1E4B7A),Color(0xFF071326)))

            // Cloud veil and limb glow lend depth without obscuring the surface map.
            drawCircle(Color(0x663EC8FF),earthR*1.015f,earthCenter,style=Stroke(width=max(1f,earthR*.035f)))
            drawCircle(
                Brush.radialGradient(listOf(Color.Transparent,Color(0x220C1426)),Offset(earthCenter.x-earthR*.3f,earthCenter.y-earthR*.35f),earthR*1.2f),
                earthR,earthCenter
            )

            val sceneScale=(small/(max(40.0,altitudeLocal+35.0)*2.7)*cameraZoom).toFloat().coerceIn(.0002f,9f)
            if(sim.trail.size>1){
                val path=Path()
                sim.trail.forEachIndexed{idx,p->
                    val q=Offset(center.x+cameraPan.x+((p.x-sim.pos.x)*sceneScale).toFloat(),center.y+cameraPan.y-((p.y-sim.pos.y)*sceneScale).toFloat())
                    if(idx==0)path.moveTo(q.x,q.y)else path.lineTo(q.x,q.y)
                }
                drawPath(path,color=Color(0x7745D7FF),style=Stroke(width=5f))
                drawPath(path,color=Cyan,style=Stroke(width=2f))
            }
            // Show the Moon as a real target only while the view is zoomed out enough to frame it.
            if(cameraZoom<=.42f){
                val moonAngle=2.0*PI*sim.time/(27.321661*86400.0)
                val moonDirection=V2(cos(moonAngle),sin(moonAngle))
                val moonScreen=Offset(center.x+cameraPan.x+moonDirection.x.toFloat()*small*.34f,center.y+cameraPan.y-moonDirection.y.toFloat()*small*.34f)
                drawCircle(Color(0x336F819A),13f,moonScreen)
                drawTexturedPlanet(null,moonScreen,7f,listOf(Color(0xFFE7E9EF),Color(0xFF929BA9),Color(0xFF3C4658)))
            }

            // Spacecraft remains central so the player can steer while panning the environment.
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
                repeat(7){i->
                    val t=i/6f
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
            if(sim.docking&&sim.targetPos!=null){
                val direction=(sim.targetPos-sim.pos).normalized()
                val target=Offset(center.x+direction.x.toFloat()*74f,center.y-direction.y.toFloat()*74f)
                drawLine(Color(0xAA45D7FF),center,target,1.5f)
                drawCircle(Cyan,10f,target,style=Stroke(width=2f))
                drawLine(Cyan,Offset(target.x-15f,target.y),Offset(target.x+15f,target.y),1.5f)
                drawLine(Cyan,Offset(target.x,target.y-15f),Offset(target.x,target.y+15f),1.5f)
            }
        }

        // Compact, translucent HUD—most of the screen stays dedicated to the world.
        Column(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(horizontal=7.dp,vertical=6.dp)){
            Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.fillMaxWidth().background(Color(0x99202C3E),RoundedCornerShape(14.dp)).padding(horizontal=7.dp,vertical=5.dp)){
                Text("‹",fontSize=29.sp,color=Ink,modifier=Modifier.clickable{onBack()}.padding(horizontal=7.dp))
                Column(Modifier.weight(1f)){
                    Text(if(sim.docking)"DOCKING" else mission,fontSize=12.sp,fontWeight=FontWeight.Black,lineHeight=15.sp)
                    Text("STAGE ${sim.stage} · T+${sim.time.roundToInt()}s",fontSize=9.sp,color=Muted,lineHeight=12.sp)
                }
                Column(horizontalAlignment=Alignment.End){
                    Text("ALT  ${"%.0f".format(altitude)} km",fontSize=11.sp,fontWeight=FontWeight.Bold)
                    Text("VEL  ${if(speed<1.0)"%.0f m/s".format(speed*1000.0) else "%.2f km/s".format(speed)}",fontSize=10.sp,color=Cyan,fontWeight=FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick=onMap,contentPadding=PaddingValues(horizontal=6.dp,vertical=4.dp)){
                    Text("MAP",fontSize=11.sp,fontWeight=FontWeight.Black)
                }
            }
            Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.padding(top=4.dp,start=4.dp)){
                Text("APO  ${if(orbit.escape)"ESCAPE" else "%.0f km".format(orbit.apoapsisAltitudeKm)}",fontSize=10.sp,color=Ink,fontWeight=FontWeight.Bold)
                Spacer(Modifier.width(11.dp))
                Text("PERI  ${if(orbit.periapsisAltitudeKm<0)"IMPACT" else "%.0f km".format(orbit.periapsisAltitudeKm)}",fontSize=10.sp,color=Ink,fontWeight=FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("${(sim.throttle*100).roundToInt()}% THROTTLE",fontSize=10.sp,color=Cyan,fontWeight=FontWeight.Black)
            }
        }

        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal=8.dp,vertical=7.dp)){
            if(sim.docking&&rangeMeters!=null&&relativeSpeed!=null){
                Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.fillMaxWidth().background(Color(0xAA0A1523),RoundedCornerShape(12.dp)).padding(horizontal=9.dp,vertical=5.dp)){
                    Column(Modifier.weight(1f)){
                        Text("RANGE",fontSize=8.sp,color=Muted,fontWeight=FontWeight.Black)
                        Text(if(rangeMeters>=1000)"%.2f km".format(rangeMeters/1000.0)else"%.1f m".format(rangeMeters),fontSize=13.sp,fontWeight=FontWeight.Black)
                    }
                    Column(Modifier.weight(1f)){
                        Text("RELATIVE SPEED",fontSize=8.sp,color=Muted,fontWeight=FontWeight.Black)
                        Text("%.2f m/s".format(relativeSpeed),fontSize=13.sp,fontWeight=FontWeight.Black,color=if(relativeSpeed<.5)Green else Orange)
                    }
                    SmallButton("APPROACH",{onTick(applyDockingApproach(sim))},Modifier.weight(1f),enabled=!sim.docked)
                    SmallButton("MATCH",{onTick(matchDockingVelocity(sim))},Modifier.weight(1f),enabled=!sim.docked)
                    SmallButton("DOCK",{val next=attemptDock(sim);onTick(next);if(next.docked)onMission("dock")},Modifier.weight(1f),enabled=!sim.docked&&rangeMeters<=5.0&&relativeSpeed<=.5)
                }
                Spacer(Modifier.height(4.dp))
            }
            Surface(color=Color(0xAA0A1422),shape=RoundedCornerShape(16.dp),modifier=Modifier.fillMaxWidth()){
                Column(Modifier.padding(horizontal=8.dp,vertical=5.dp)){
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Text("THR",fontSize=9.sp,color=Muted,fontWeight=FontWeight.Black)
                        Slider(value=sim.throttle.toFloat(),onValueChange={onTick(sim.copy(throttle=it.toDouble()))},valueRange=0f..1f,modifier=Modifier.weight(1f).height(31.dp),colors=SliderDefaults.colors(thumbColor=Cyan,activeTrackColor=Cyan,inactiveTrackColor=Color(0x663C536D)))
                        Text("${(sim.throttle*100).roundToInt()}%",fontSize=10.sp,color=Ink,fontWeight=FontWeight.Black)
                    }
                    Row(horizontalArrangement=Arrangement.spacedBy(5.dp),verticalAlignment=Alignment.CenterVertically,modifier=Modifier.fillMaxWidth()){
                        SmallButton("◀",{
                            pitchOffset=(pitchOffset-5f).coerceIn(0f,180f)
                            onTick(sim.copy(guidance=Guidance.MANUAL))
                        },Modifier.weight(.8f))
                        SmallButton("STAGE",{onTick(separateStage(sim))},Modifier.weight(1.15f),enabled=sim.parts.contains(PartType.DECOUPLER))
                        SmallButton("▶",{
                            pitchOffset=(pitchOffset+5f).coerceIn(0f,180f)
                            onTick(sim.copy(guidance=Guidance.MANUAL))
                        },Modifier.weight(.8f))
                        SmallButton(if(paused)"▶" else "Ⅱ",{paused=!paused},Modifier.weight(.8f))
                        SmallButton(if(showAdvanced)"LESS" else "MORE",{showAdvanced=!showAdvanced},Modifier.weight(1f))
                    }
                    if(showAdvanced){
                        Spacer(Modifier.height(3.dp))
                        Row(horizontalArrangement=Arrangement.spacedBy(5.dp),modifier=Modifier.fillMaxWidth()){
                            SmallButton("PROGRADE",{onTick(sim.copy(guidance=Guidance.PROGRADE))},Modifier.weight(1f))
                            SmallButton("RETRO",{onTick(sim.copy(guidance=Guidance.RETROGRADE))},Modifier.weight(1f))
                            SmallButton("WARP ×$timeWarp",{timeWarp=if(timeWarp==1)5 else if(timeWarp==5)20 else 1},Modifier.weight(1f))
                            SmallButton(if(sim.parachuteDeployed)"CHUTE ✓" else "CHUTE",{
                                if(PartType.PARACHUTE in activeStageParts(sim.parts)&&altitude in 0.0..15.0&&speed<.30)onTick(sim.copy(parachuteDeployed=true))
                            },Modifier.weight(1f),enabled=PartType.PARACHUTE in activeStageParts(sim.parts)&&altitude in 0.0..15.0&&speed<.30&&!sim.parachuteDeployed)
                            SmallButton("${cameraZoom.toInt()}×",{cameraZoom=1f;cameraPan=Offset.Zero},Modifier.weight(.8f))
                        }
                        Spacer(Modifier.height(3.dp))
                        Text("TWR %.2f · MAX Q %.0f kPa · HEAT %.0f%%".format(twr,sim.maxDynamicPressureKpa,(sim.heat*100).coerceIn(0.0,150.0)),fontSize=9.sp,color=Muted,modifier=Modifier.padding(start=4.dp))
                    }
                }
            }
            if(sim.crashed||sim.landed||sim.docked){
                Spacer(Modifier.height(4.dp))
                Text(
                    when{
                        sim.crashed->"VEHICLE LOST · CHECK STAGING / HEAT / RE-ENTRY"
                        sim.landed->if(sim.maxAltitudeKm>=80)"SAFE RECOVERY · MISSION RECORDED" else "SOFT LANDING · FLIGHT RECORDED"
                        else->"DOCKING COMPLETE · RELATIVE MOTION MATCHED"
                    },
                    modifier=Modifier.fillMaxWidth().background(Color(0xCC101923),RoundedCornerShape(9.dp)).padding(8.dp),
                    fontSize=10.sp,fontWeight=FontWeight.Black,color=if(sim.crashed)Red else Green
                )
            }
        }
    }
}

@Composable
private fun SmallButton(text:String,onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true){
    Button(
        onClick=onClick,enabled=enabled,modifier=modifier.heightIn(min=40.dp),
        shape=RoundedCornerShape(10.dp),contentPadding=PaddingValues(horizontal=4.dp,vertical=6.dp),
        colors=ButtonDefaults.buttonColors(
            containerColor=Color(0xAA142236),
            contentColor=Color(0xFFF4F7FF),
            disabledContainerColor=Color(0x55101A28),
            disabledContentColor=Color(0xFF7F8CA0)
        )
    ){Text(text,fontSize=10.sp,lineHeight=12.sp,fontWeight=FontWeight.Black)}
}

@Composable
private fun MapScreen(sim:SimState,earthTexture:ImageBitmap?,marsTexture:ImageBitmap?,onBack:()->Unit){
    var cameraZoom by rememberSaveable{mutableFloatStateOf(.62f)}
    var cameraPan by remember{mutableStateOf(Offset.Zero)}
    Box(Modifier.fillMaxSize().background(Color(0xFF040713))){
        Canvas(
            Modifier.fillMaxSize().pointerInput(Unit){
                detectTransformGestures{_,pan,zoom,_->
                    cameraZoom=(cameraZoom*zoom).coerceIn(.12f,4.0f)
                    cameraPan=Offset(
                        (cameraPan.x+pan.x).coerceIn(-size.width*.65f,size.width*.65f),
                        (cameraPan.y+pan.y).coerceIn(-size.height*.60f,size.height*.60f)
                    )
                }
            }
        ){
            val small=min(size.width,size.height)
            val center=Offset(size.width*.5f+cameraPan.x,size.height*.52f+cameraPan.y)
            val mapLimit=small*.44f
            drawRect(Brush.verticalGradient(listOf(Color(0xFF050A18),Color(0xFF0B1B31),Color(0xFF040711))),size=Size(size.width,size.height))
            drawOval(Brush.radialGradient(listOf(Color(0x1A536EB0),Color.Transparent),Offset(size.width*.69f,size.height*.24f),small*.75f),Offset(size.width*.1f,size.height*.01f),Size(size.width*.85f,size.height*.75f))
            repeat(130){i->
                val sx=((i*71+11)%997)/997f*size.width
                val sy=((i*37+23)%991)/991f*size.height
                drawCircle(if(i%13==0)Color(0xFFB5E9FF) else Color.White,if(i%7==0)1.5f else .65f,Offset(sx,sy),alpha=if(i%5==0).72f else .28f)
            }
            fun baseRadius(radiusKm:Double):Float{
                val er=EARTH_RADIUS_KM
                return if(radiusKm<=er*4.0)
                    (16.0+(radiusKm-er).coerceAtLeast(0.0)/er*12.0).toFloat()
                else
                    (28.0+ln(radiusKm/(er*4.0))*24.0).toFloat()
            }
            fun plotRadius(radiusKm:Double)=(baseRadius(radiusKm)*cameraZoom).coerceAtMost(mapLimit*1.6f)
            // Reference orbits make the navigation scale readable while still fitting the inner system.
            listOf(EARTH_RADIUS_KM+200.0,EARTH_RADIUS_KM+35786.0,MOON_ORBIT_KM).forEachIndexed{idx,r->
                drawCircle(Color(if(idx==2)0x445A7BA5L else 0x332F4B71L),plotRadius(r),center,style=Stroke(width=1f))
            }
            val earthR=(15f*cameraZoom*cameraZoom).coerceIn(3f,small*.30f)
            drawCircle(Brush.radialGradient(listOf(Color(0x6649BFFF),Color.Transparent),center,earthR*2.1f),earthR*2.1f,center)
            drawTexturedPlanet(earthTexture,center,earthR,listOf(Color(0xFF448FC7),Color(0xFF1D4E80),Color(0xFF071326)))
            val moonAngle=2.0*PI*sim.time/(27.321661*86400.0)
            val moonR=plotRadius(MOON_ORBIT_KM)
            val moonSpot=Offset(center.x+cos(moonAngle).toFloat()*moonR,center.y-sin(moonAngle).toFloat()*moonR)
            drawCircle(Color(0x339EA8BD),max(4f,6f*cameraZoom),moonSpot)
            drawTexturedPlanet(null,moonSpot,max(2f,4.5f*cameraZoom),listOf(Color(0xFFF0F0EC),Color(0xFF969EAC),Color(0xFF343D4B)))
            val marsDistance=227_940_000.0
            val marsAngle=0.8
            val marsR=plotRadius(marsDistance)
            val marsSpot=Offset(center.x+cos(marsAngle).toFloat()*marsR,center.y-sin(marsAngle).toFloat()*marsR)
            drawCircle(Color(0x44D36B4D),max(4f,9f*cameraZoom),marsSpot)
            drawTexturedPlanet(marsTexture,marsSpot,max(3f,7f*cameraZoom),listOf(Color(0xFFD58B61),Color(0xFF8C4637),Color(0xFF3B1E20)))

            if(sim.trail.size>1){
                val path=Path()
                sim.trail.takeLast(600).forEachIndexed{idx,p->
                    val radius=p.mag().coerceAtLeast(1.0)
                    val angle=atan2(p.y,p.x)
                    val rp=plotRadius(radius)
                    val q=Offset(center.x+cos(angle).toFloat()*rp,center.y-sin(angle).toFloat()*rp)
                    if(idx==0)path.moveTo(q.x,q.y)else path.lineTo(q.x,q.y)
                }
                drawPath(path,color=Color(0x9955CFFF),style=Stroke(width=5f))
                drawPath(path,color=Cyan,style=Stroke(width=2f))
            }
            val currentAngle=atan2(sim.pos.y,sim.pos.x)
            val currentR=plotRadius(sim.pos.mag())
            val vehicle=Offset(center.x+cos(currentAngle).toFloat()*currentR,center.y-sin(currentAngle).toFloat()*currentR)
            drawCircle(Color.White,4.2f,vehicle)
            drawCircle(Cyan,10f,vehicle,style=Stroke(width=1.5f))
            if(sim.targetPos!=null&&sim.docking){
                val a=atan2(sim.targetPos.y,sim.targetPos.x)
                val r=plotRadius(sim.targetPos.mag())
                drawCircle(Color(0xFF45D7FF),5f,Offset(center.x+cos(a).toFloat()*r,center.y-sin(a).toFloat()*r),style=Stroke(width=2f))
            }
        }
        Column(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(8.dp)){
            Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.fillMaxWidth().background(Color(0xAA071320),RoundedCornerShape(14.dp)).padding(horizontal=6.dp,vertical=4.dp)){
                Text("‹",fontSize=30.sp,modifier=Modifier.clickable{onBack()}.padding(horizontal=8.dp),color=Ink)
                Column(Modifier.weight(1f)){
                    Text("SOLAR SYSTEM",fontWeight=FontWeight.Black,fontSize=14.sp)
                    Text("Pinch to zoom · drag to explore",color=Muted,fontSize=10.sp)
                }
                TextButton(onClick={cameraZoom=.48f;cameraPan=Offset.Zero},contentPadding=PaddingValues(horizontal=6.dp)){Text("UNIVERSE",fontSize=9.sp,fontWeight=FontWeight.Black)}
                TextButton(onClick={cameraZoom=3.2f;cameraPan=Offset.Zero},contentPadding=PaddingValues(horizontal=6.dp)){Text("EARTH",fontSize=9.sp,fontWeight=FontWeight.Black)}
            }
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp),modifier=Modifier.padding(start=8.dp,top=6.dp)){
                Text("EARTH",fontSize=9.sp,color=Color(0xFF8ED9FF),fontWeight=FontWeight.Black)
                Text("·",fontSize=9.sp,color=Muted)
                Text("MOON",fontSize=9.sp,color=Color(0xFFE2E5EC),fontWeight=FontWeight.Black)
                Text("·",fontSize=9.sp,color=Muted)
                Text("MARS",fontSize=9.sp,color=Color(0xFFFFA07B),fontWeight=FontWeight.Black)
            }
        }
        Surface(color=Color(0xAA081321),shape=RoundedCornerShape(12.dp),modifier=Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(8.dp)){
            Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.padding(horizontal=12.dp,vertical=8.dp)){
                Column(Modifier.weight(1f)){
                    Text(if(cameraZoom<.8f)"SYSTEM VIEW" else if(cameraZoom<2f)"PLANETARY VIEW" else "EARTH DETAIL",fontSize=10.sp,color=Cyan,fontWeight=FontWeight.Black)
                    Text("Zoom ${"%.2f".format(cameraZoom)}× · Orbit values remain in real units",fontSize=10.sp,color=Muted)
                }
                SmallButton("CENTER",{cameraPan=Offset.Zero},Modifier.width(78.dp))
            }
        }
    }
}

private fun worldToScreen(p:V2,rocket:V2,center:Offset,scale:Float):Offset{
    return Offset(center.x+((p.x-rocket.x)*scale).toFloat(),center.y-((p.y-rocket.y)*scale).toFloat())
}
