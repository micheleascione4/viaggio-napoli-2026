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
import org.json.JSONArray
import org.json.JSONObject
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
    // Top-to-bottom order. The lowest stage lights first; upper stages ignite after separation.
    PartType.NOSE,PartType.CAPSULE,PartType.HEATSHIELD,PartType.PARACHUTE,
    PartType.TANK,PartType.TANK,PartType.VACUUM_ENGINE,PartType.DECOUPLER,
    PartType.TANK,PartType.TANK,PartType.VACUUM_ENGINE,PartType.DECOUPLER,
    PartType.TANK,PartType.TANK,PartType.VACUUM_ENGINE,PartType.DECOUPLER,
    PartType.TANK,PartType.TANK,PartType.HEAVY_ENGINE,PartType.FIN
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
        val savedBlueprints:Map<String,Rocket> = runCatching{
            val json=JSONObject(preferences.getString("saved_blueprints","{}")?:"{}")
            val restored=mutableMapOf<String,Rocket>()
            val names=json.keys()
            while(names.hasNext()){
                val name=names.next()
                val array=json.optJSONArray(name)?:continue
                val parts=(0 until array.length()).mapNotNull{index->
                    runCatching{PartType.valueOf(array.getString(index))}.getOrNull()
                }
                if(name.isNotBlank()&&parts.isNotEmpty())restored[name]=Rocket(parts)
            }
            restored.toMap()
        }.getOrDefault(emptyMap())
        val defaults=CareerState()
        val initialCareer=CareerState(
            funds=preferences.getInt("career_funds",defaults.funds),
            science=preferences.getInt("career_science",defaults.science),
            reputation=preferences.getInt("career_reputation",defaults.reputation),
            unlocked=(preferences.getStringSet("career_unlocked",null)?.toSet()?:defaults.unlocked) + if(PartType.HEAVY_ENGINE in initialRocket.parts) setOf("heavy") else emptySet(),
            completedContracts=preferences.getStringSet("career_contracts",null)?.toSet().orEmpty()
        )
        val initialMissions=preferences.getStringSet("flight_achievements",null)?.toSet().orEmpty()
        setContent{
            Box(Modifier.fillMaxSize().background(Bg).windowInsetsPadding(WindowInsets.safeDrawing)){
                OrbitalFoundryApp(
                    initialRocket=initialRocket,
                    initialBlueprints=savedBlueprints,
                    initialMissions=initialMissions,
                    initialCareer=initialCareer,
                    onTutorialComplete={preferences.edit().putBoolean("tutorial_seen",true).apply()},
                    onSaveRocket={rocket->preferences.edit().putString("rocket_parts",rocket.parts.joinToString(","){it.name}).apply()},
                    onSaveBlueprints={blueprints->
                        val json=JSONObject()
                        blueprints.toSortedMap().forEach{(name,blueprint)->
                            json.put(name,JSONArray(blueprint.parts.map{it.name}))
                        }
                        preferences.edit().putString("saved_blueprints",json.toString()).apply()
                    },
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
    initialBlueprints:Map<String,Rocket>,
    initialMissions:Set<String>,
    initialCareer:CareerState,
    onTutorialComplete:()->Unit,
    onSaveRocket:(Rocket)->Unit,
    onSaveBlueprints:(Map<String,Rocket>)->Unit,
    onSaveMissions:(Set<String>)->Unit,
    onSaveCareer:(CareerState)->Unit
){
    val context=LocalContext.current
    val earthTexture=remember{loadPlanetTexture(context,"earth.png")}
    val marsTexture=remember{loadPlanetTexture(context,"mars.jpg")}
    MaterialTheme(colorScheme=darkColorScheme(background=Bg,surface=Panel,primary=Color(0xFF3479B5),secondary=Cyan,onBackground=Ink,onSurface=Ink)){
        var screen by rememberSaveable{mutableStateOf(Screen.HOME)}
        var rocket by remember{mutableStateOf(initialRocket)}
        var blueprints by remember{mutableStateOf(initialBlueprints)}
        var sim by remember{mutableStateOf<SimState?>(null)}
        var missions by remember{mutableStateOf(initialMissions)}
        var career by remember{mutableStateOf(initialCareer)}
        var sandboxMode by rememberSaveable{mutableStateOf(false)}
        LaunchedEffect(rocket){onSaveRocket(rocket)}
        LaunchedEffect(blueprints){onSaveBlueprints(blueprints)}
        LaunchedEffect(missions){onSaveMissions(missions)}
        LaunchedEffect(career){onSaveCareer(career)}
        Column(Modifier.fillMaxSize().background(Bg)){
            Box(Modifier.weight(1f).fillMaxWidth()){
        when(screen){
            Screen.HOME->HomeScreen(rocket,missions,career,
                {sandboxMode=false;screen=Screen.BUILD},
                {if(rocket.hasEngine&&rocket.hasCapsule){sim=launchState(rocket);screen=Screen.FLIGHT}},
                {screen=Screen.MISSIONS},
                {screen=Screen.TUTORIAL},
                {sandboxMode=true;screen=Screen.BUILD},
                {if(rocket.hasDockingPort&&rocket.hasRcs){sim=beginDockingPractice(rocket);screen=Screen.FLIGHT}}
            )
            Screen.BUILD->BuilderScreen(
                rocket,career.unlocked,sandboxMode,blueprints,
                {name->blueprints=blueprints+(name to rocket)},
                {name->blueprints[name]?.let{rocket=it}},
                {name->blueprints=blueprints-name},
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
            if(screen==Screen.HOME||screen==Screen.BUILD||screen==Screen.MISSIONS){
                MainNavigationBar(current=screen,mapAvailable=sim!=null){destination->
                    when(destination){
                        Screen.HOME->screen=Screen.HOME
                        Screen.BUILD->screen=Screen.BUILD
                        Screen.MISSIONS->screen=Screen.MISSIONS
                        Screen.MAP->{if(sim!=null)screen=Screen.MAP}
                        else->Unit
                    }
                }
            }
        }
    }
}


@Composable
private fun MainNavigationBar(current:Screen,mapAvailable:Boolean,onNavigate:(Screen)->Unit){
    NavigationBar(
        containerColor=Color(0xFF081320),
        contentColor=Ink,
        tonalElevation=0.dp,
        modifier=Modifier.fillMaxWidth()
    ){
        val destinations=listOf(
            Triple(Screen.HOME,"HOME","⌂"),
            Triple(Screen.BUILD,"BUILD","✳"),
            Triple(Screen.MISSIONS,"CAREER","◈"),
            Triple(Screen.MAP,"MAP","◎")
        )
        destinations.forEach{(destination,label,glyph)->
            NavigationBarItem(
                selected=current==destination,
                onClick={onNavigate(destination)},
                enabled=destination!=Screen.MAP||mapAvailable,
                icon={Text(glyph,fontSize=19.sp,fontWeight=FontWeight.Bold)},
                label={Text(label,fontSize=9.sp,fontWeight=FontWeight.Black,letterSpacing=.4.sp)},
                alwaysShowLabel=true,
                colors=NavigationBarItemDefaults.colors(
                    selectedIconColor=Cyan,
                    selectedTextColor=Cyan,
                    selectedIndicatorColor=Color(0xFF15354B),
                    unselectedIconColor=Muted,
                    unselectedTextColor=Muted,
                    disabledIconColor=Color(0xFF39495B),
                    disabledTextColor=Color(0xFF39495B)
                )
            )
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
    val firstEngine=list.indexOfFirst{it==PartType.ENGINE||it==PartType.VACUUM_ENGINE||it==PartType.HEAVY_ENGINE||it==PartType.ION_ENGINE}
    val at=when{
        firstTank>=0->firstTank
        firstEngine>=0->firstEngine
        else->list.size
    }.coerceIn(0,list.size)
    // A separator is needed only when an existing stack will remain below the new upper stage.
    // On an empty/new vehicle, keep the first stage active instead of creating an empty active stage.
    val modules=listOf(PartType.TANK,PartType.TANK,PartType.VACUUM_ENGINE)
    val upperStage=if(at<list.size)modules+PartType.DECOUPLER else modules
    list.addAll(at,upperStage)
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
    val bg=Color(0xFF070C14)
    val surface=Color(0xFF101A28)
    val ink=Color(0xFFEAF2FB)
    val muted=Color(0xFF91A4B9)
    val accent=Color(0xFF66B9E9)
    Column(Modifier.fillMaxSize().background(bg).verticalScroll(rememberScrollState()).padding(horizontal=16.dp,vertical=12.dp)){
        Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.fillMaxWidth()){
            Column(Modifier.weight(1f)){
                Row(verticalAlignment=Alignment.CenterVertically){
                    Box(Modifier.size(7.dp).background(Color(0xFF63DDB0),CircleShape))
                    Text(" ORBITAL PROGRAM  /  01",fontSize=9.sp,color=muted,fontWeight=FontWeight.Black,letterSpacing=1.1.sp)
                }
                Text("ORVITARY",fontSize=28.sp,fontWeight=FontWeight.Black,lineHeight=32.sp,letterSpacing=1.5.sp,color=ink)
                Text("DESIGN. LAUNCH. EXPLORE.",color=accent,fontSize=10.sp,fontWeight=FontWeight.Bold,letterSpacing=1.1.sp)
            }
            TextButton(onClick=onTutorial,contentPadding=PaddingValues(horizontal=10.dp,vertical=7.dp),colors=ButtonDefaults.textButtonColors(contentColor=ink)){
                Text("FLIGHT SCHOOL",fontSize=9.sp,fontWeight=FontWeight.Black)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.fillMaxWidth().padding(bottom=6.dp)){
            Text("CURRENT VEHICLE",fontSize=10.sp,color=muted,fontWeight=FontWeight.Black,letterSpacing=1.2.sp)
            Spacer(Modifier.weight(1f))
            Surface(color=if(rocket.hasEngine&&rocket.hasCapsule)Color(0xFF123329) else Color(0xFF3A2630),shape=RoundedCornerShape(30.dp)){
                Text(if(rocket.hasEngine&&rocket.hasCapsule)"● READY" else "● INCOMPLETE",fontSize=9.sp,color=if(rocket.hasEngine&&rocket.hasCapsule)Color(0xFF63DDB0) else Color(0xFFFF9AAB),fontWeight=FontWeight.Black,modifier=Modifier.padding(horizontal=10.dp,vertical=5.dp))
            }
        }
        Box(Modifier.fillMaxWidth().height(232.dp).background(Color(0xFF0C1A29),RoundedCornerShape(14.dp)).border(1.dp,Color(0xFF243B51),RoundedCornerShape(14.dp))){
            RocketStackPreview(rocket,Modifier.fillMaxSize())
            Surface(color=Color(0xDD091522),shape=RoundedCornerShape(bottomEnd=11.dp),modifier=Modifier.align(Alignment.TopStart)){
                Column(Modifier.padding(horizontal=11.dp,vertical=8.dp)){
                    Text("ACTIVE BLUEPRINT",fontSize=8.sp,color=accent,fontWeight=FontWeight.Black,letterSpacing=1.sp)
                    Text(if(rocket.hasEngine&&rocket.hasCapsule)"Launch vehicle" else "Unfinished assembly",fontSize=13.sp,color=ink,fontWeight=FontWeight.Black)
                }
            }
            Surface(color=Color(0xEE0A1420),shape=RoundedCornerShape(11.dp),modifier=Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(7.dp)){
                Row(Modifier.padding(horizontal=8.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically){
                    Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally){
                        Text("WET MASS",fontSize=8.sp,color=muted,fontWeight=FontWeight.Bold)
                        Text("%.1f t".format(rocket.dryMass+rocket.fuel),fontSize=12.sp,color=ink,fontWeight=FontWeight.Black)
                    }
                    Box(Modifier.width(1.dp).height(26.dp).background(Color(0xFF2A3D50)))
                    Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally){
                        Text("PROPELLANT",fontSize=8.sp,color=muted,fontWeight=FontWeight.Bold)
                        Text("%.0f t".format(rocket.fuel),fontSize=12.sp,color=ink,fontWeight=FontWeight.Black)
                    }
                    Box(Modifier.width(1.dp).height(26.dp).background(Color(0xFF2A3D50)))
                    Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally){
                        Text("DELTA-V",fontSize=8.sp,color=muted,fontWeight=FontWeight.Bold)
                        Text("%.2f km/s".format(rocket.deltaV),fontSize=12.sp,color=accent,fontWeight=FontWeight.Black)
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick=onBuild,modifier=Modifier.fillMaxWidth().height(51.dp),shape=RoundedCornerShape(11.dp),
            contentPadding=PaddingValues(12.dp),colors=ButtonDefaults.buttonColors(containerColor=accent,contentColor=Color(0xFF061522))
        ){
            Text("OPEN VEHICLE WORKSHOP    ↗",fontWeight=FontWeight.Black,letterSpacing=.6.sp,fontSize=12.sp)
        }
        Spacer(Modifier.height(7.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth()){
            OutlinedButton(
                onClick=onSandbox,modifier=Modifier.weight(1f).height(47.dp),shape=RoundedCornerShape(10.dp),
                colors=ButtonDefaults.outlinedButtonColors(contentColor=ink),border=androidx.compose.foundation.BorderStroke(1.dp,Color(0xFF385274))
            ){Text("SANDBOX",fontWeight=FontWeight.Black,fontSize=11.sp)}
            Button(
                onClick=onLaunch,enabled=rocket.hasEngine&&rocket.hasCapsule,modifier=Modifier.weight(1f).height(47.dp),
                shape=RoundedCornerShape(10.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF17364A),contentColor=accent)
            ){Text("QUICK LAUNCH  ▶",fontWeight=FontWeight.Black,fontSize=10.sp)}
        }
        Spacer(Modifier.height(12.dp))
        Surface(color=surface,shape=RoundedCornerShape(13.dp),modifier=Modifier.fillMaxWidth().border(1.dp,Color(0xFF24394E),RoundedCornerShape(13.dp))){
            Column(Modifier.padding(13.dp)){
                Row(verticalAlignment=Alignment.CenterVertically){
                    Column(Modifier.weight(1f)){
                        Text("CAREER CONTROL",fontSize=10.sp,color=accent,fontWeight=FontWeight.Black,letterSpacing=1.1.sp)
                        Text("Research, contracts and progression",fontSize=12.sp,color=ink,fontWeight=FontWeight.SemiBold)
                    }
                    TextButton(onClick=onMissions,contentPadding=PaddingValues(horizontal=7.dp,vertical=4.dp)){Text("OPEN  ↗",fontSize=10.sp,fontWeight=FontWeight.Black,color=accent)}
                }
                Spacer(Modifier.height(9.dp))
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){
                    Column(Modifier.weight(1f).background(Color(0xFF0A1420),RoundedCornerShape(9.dp)).padding(9.dp)){
                        Text("FUNDS",fontSize=8.sp,color=muted,fontWeight=FontWeight.Bold)
                        Text("€"+(career.funds/1000)+"K",fontSize=16.sp,color=ink,fontWeight=FontWeight.Black)
                    }
                    Column(Modifier.weight(1f).background(Color(0xFF0A1420),RoundedCornerShape(9.dp)).padding(9.dp)){
                        Text("SCIENCE",fontSize=8.sp,color=muted,fontWeight=FontWeight.Bold)
                        Text(career.science.toString()+" SCI",fontSize=16.sp,color=ink,fontWeight=FontWeight.Black)
                    }
                    Column(Modifier.weight(1f).background(Color(0xFF0A1420),RoundedCornerShape(9.dp)).padding(9.dp)){
                        Text("TECH",fontSize=8.sp,color=muted,fontWeight=FontWeight.Bold)
                        Text(career.unlocked.size.toString(),fontSize=16.sp,color=ink,fontWeight=FontWeight.Black)
                    }
                }
                Spacer(Modifier.height(9.dp))
                Row(verticalAlignment=Alignment.CenterVertically){
                    Text("MISSION RECORD",fontSize=9.sp,color=muted,fontWeight=FontWeight.Black,letterSpacing=.8.sp)
                    Spacer(Modifier.weight(1f))
                    Text(missions.size.toString()+" achievements",fontSize=10.sp,color=ink,fontWeight=FontWeight.Bold)
                }
                Spacer(Modifier.height(5.dp))
                Row(horizontalArrangement=Arrangement.spacedBy(7.dp),modifier=Modifier.fillMaxWidth()){
                    OutlinedButton(onClick=onMissions,modifier=Modifier.weight(1f).height(38.dp),contentPadding=PaddingValues(horizontal=4.dp),shape=RoundedCornerShape(8.dp),colors=ButtonDefaults.outlinedButtonColors(contentColor=ink)){Text("CONTRACTS + TECH",fontWeight=FontWeight.Black,fontSize=9.sp)}
                    OutlinedButton(onClick=onDocking,enabled=rocket.hasDockingPort&&rocket.hasRcs,modifier=Modifier.weight(1f).height(38.dp),contentPadding=PaddingValues(horizontal=4.dp),shape=RoundedCornerShape(8.dp),colors=ButtonDefaults.outlinedButtonColors(contentColor=ink)){Text("DOCKING RANGE",fontWeight=FontWeight.Black,fontSize=9.sp)}
                }
            }
        }
        Spacer(Modifier.height(8.dp))
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

private fun stackPreviewScale(height:Float,count:Int,density:Float,maxVisibleParts:Int=Int.MAX_VALUE):Float{
    if(count<=0)return 1f
    // The workshop intentionally lets long rockets extend beyond the viewport; they can be panned.
    val available=(height-110f*density).coerceAtLeast(60f*density)
    val fitCount=min(count,maxVisibleParts).coerceAtLeast(1)
    return min(1f,available/(fitCount*94f*density)).coerceIn(.12f,1f)
}

private fun stackInsertionIndex(y:Float,height:Float,count:Int,density:Float,verticalPan:Float=0f,maxVisibleParts:Int=Int.MAX_VALUE):Int{
    if(count<=0)return 0
    val scale=stackPreviewScale(height,count,density,maxVisibleParts)
    val partH=88f*density*scale
    val step=94f*density*scale
    val totalHeight=count*partH+(count-1).coerceAtLeast(0)*6f*density*scale
    val topCenter=(height-totalHeight)/2f+partH/2f+verticalPan
    val bottomCenter=topCenter+(count-1)*step
    if(y<topCenter-partH/2f)return 0
    if(y>bottomCenter+partH/2f)return count
    val row=((y-topCenter)/step).roundToInt().coerceIn(0,count-1)
    val center=topCenter+row*step
    return (row+if(y>center)1 else 0).coerceIn(0,count)
}


@Composable
private fun RocketStackPreview(
    rocket:Rocket,
    modifier:Modifier=Modifier,
    onReorder:((Int,Int)->Unit)?=null,
    verticalPan:Float=0f,
    onPan:((Float)->Unit)?=null,
    maxVisibleParts:Int=Int.MAX_VALUE
){
    val density=androidx.compose.ui.platform.LocalDensity.current.density
    val interactionModifier=if(onReorder!=null||onPan!=null) Modifier.pointerInput(rocket.parts,onReorder,onPan,density,maxVisibleParts){
        var from=-1
        var to=-1
        var panMode=false
        detectDragGestures(
            onDragStart={point->
                val count=rocket.parts.size
                val scale=stackPreviewScale(size.height.toFloat(),count,density,maxVisibleParts)
                val partH=88f*density*scale
                val step=94f*density*scale
                val stackHeight=count*partH+(count-1).coerceAtLeast(0)*6f*density*scale
                val topCenter=(size.height-stackHeight)/2f+partH/2f+verticalPan
                val row=((point.y-topCenter)/step).roundToInt()
                if(onReorder!=null&&row in 0 until count&&abs(point.y-(topCenter+row*step))<=partH*.65f){
                    from=row
                    to=row
                    panMode=false
                }else{
                    from=-1
                    to=-1
                    panMode=true
                }
            },
            onDrag={change,delta->
                change.consume()
                if(panMode){
                    onPan?.invoke(delta.y)
                }else if(rocket.parts.isNotEmpty()&&from>=0){
                    val count=rocket.parts.size
                    val scale=stackPreviewScale(size.height.toFloat(),count,density,maxVisibleParts)
                    val partH=88f*density*scale
                    val step=94f*density*scale
                    val stackHeight=count*partH+(count-1).coerceAtLeast(0)*6f*density*scale
                    val topCenter=(size.height-stackHeight)/2f+partH/2f+verticalPan
                    to=((change.position.y-topCenter)/step).roundToInt().coerceIn(0,count-1)
                }
            },
            onDragEnd={
                val source=from
                val destination=to
                from=-1
                to=-1
                if(!panMode&&source>=0&&destination>=0&&source!=destination)onReorder?.invoke(source,destination-source)
                panMode=false
            },
            onDragCancel={from=-1;to=-1;panMode=false}
        )
    }else Modifier
    Canvas(modifier.fillMaxSize().then(interactionModifier)){
        drawRect(Brush.verticalGradient(listOf(Color(0xFF10263C),Color(0xFF0B1B2D),Color(0xFF071321))),size=Size(size.width,size.height))
        val grid=42f*density
        for(x in 0..(size.width/grid).toInt())drawLine(Color(0x223C759F),Offset(x*grid,0f),Offset(x*grid,size.height),max(1f,density*.35f))
        for(y in 0..(size.height/grid).toInt())drawLine(Color(0x223C759F),Offset(0f,y*grid),Offset(size.width,y*grid),max(1f,density*.35f))
        drawLine(Color(0x3345D7FF),Offset(size.width*.5f,0f),Offset(size.width*.5f,size.height),max(1f,density*.35f))
        val count=rocket.parts.size
        val scale=stackPreviewScale(size.height,count,density,maxVisibleParts)
        val partH=88f*density*scale
        val partGap=6f*density*scale
        val partW=min(82f*density,size.width*.42f)*scale
        val left=(size.width-partW)*.5f
        val stackHeight=count*partH+(count-1).coerceAtLeast(0)*partGap
        var y=(size.height+stackHeight)/2f-partH+verticalPan
        rocket.parts.asReversed().forEach{part->
            drawRocketComponent(part,left,y,partW,partH)
            y-=partH+partGap
        }
        val platformY=if(count>0)(size.height+stackHeight)/2f+verticalPan+14f*density*scale else size.height*.82f
        drawRoundRect(Color(0xFF1A354D),Offset(size.width*.27f,platformY),Size(size.width*.46f,8f*density*scale),CornerRadius(3f*density*scale))
        drawLine(Color(0xFF4B8AB7),Offset(size.width*.18f,platformY+9f*density*scale),Offset(size.width*.82f,platformY+9f*density*scale),1.5f*density*scale)
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
    blueprints:Map<String,Rocket>,
    onSaveBlueprint:(String)->Unit,onLoadBlueprint:(String)->Unit,onDeleteBlueprint:(String)->Unit,
    onAdd:(PartType)->Unit,onDrop:(PartType,Int)->Unit,onAddStage:()->Unit,
    onRemove:(Int)->Unit,onMove:(Int,Int)->Unit,onClear:()->Unit,onBack:()->Unit,onLaunch:()->Unit
){
    var showStack by rememberSaveable{mutableStateOf(false)}
    var showStageEditor by rememberSaveable{mutableStateOf(false)}
    var showBlueprintLibrary by rememberSaveable{mutableStateOf(false)}
    var showAnalysis by rememberSaveable{mutableStateOf(false)}
    var selectedCategory by rememberSaveable{mutableStateOf("ALL")}
    var canvasPan by rememberSaveable{mutableFloatStateOf(0f)}
    val unlocked=if(sandboxMode)CareerDatabase.tech.map{it.id}.toSet() else unlockedTech
    var rootBounds by remember{mutableStateOf<Rect?>(null)}
    var viewportBounds by remember{mutableStateOf<Rect?>(null)}
    var draggingPart by remember{mutableStateOf<PartType?>(null)}
    var dragPosition by remember{mutableStateOf<Offset?>(null)}
    val density=androidx.compose.ui.platform.LocalDensity.current.density
    val dragHalf=with(androidx.compose.ui.platform.LocalDensity.current){40.dp.toPx()}
    val categories=listOf("ALL","STRUCTURE","PROPULSION","CONTROL","UTILITY")
    val parts=when(selectedCategory){
        "STRUCTURE"->listOf(PartType.NOSE,PartType.FAIRING,PartType.CAPSULE,PartType.PROBE_CORE,PartType.HEATSHIELD)
        "PROPULSION"->listOf(PartType.TANK,PartType.ENGINE,PartType.VACUUM_ENGINE,PartType.HEAVY_ENGINE,PartType.ION_ENGINE,PartType.ION_TANK,PartType.DECOUPLER)
        "CONTROL"->listOf(PartType.FIN,PartType.RCS,PartType.LANDING_LEGS)
        "UTILITY"->listOf(PartType.PARACHUTE,PartType.DOCKING_PORT,PartType.SOLAR)
        else->PartType.entries.toList()
    }
    val stats=remember(rocket.parts){analyzeVehicle(rocket)}
    val ink=Color(0xFFF0F5FC)
    val muted=Color(0xFF9DB0C5)
    val cyan=Color(0xFF63D7FF)
    val line=Color(0xFF29445D)
    Box(Modifier.fillMaxSize().background(Color(0xFF050B14)).onGloballyPositioned{rootBounds=it.boundsInWindow()}){
        Column(Modifier.fillMaxSize()){
            Row(Modifier.fillMaxWidth().height(56.dp).background(Color(0xFF0B1624)).padding(horizontal=10.dp,vertical=5.dp),verticalAlignment=Alignment.CenterVertically){
                Surface(color=Color(0xFF172A3D),shape=RoundedCornerShape(12.dp),modifier=Modifier.size(42.dp).clickable{onBack()}){
                    Box(contentAlignment=Alignment.Center){Text("‹",fontSize=31.sp,color=ink,lineHeight=32.sp)}
                }
                Column(Modifier.weight(1f).padding(start=10.dp)){
                    Text("VEHICLE WORKSHOP",fontSize=13.sp,fontWeight=FontWeight.Black,letterSpacing=.8.sp,color=ink,lineHeight=16.sp)
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Box(Modifier.size(6.dp).background(if(sandboxMode)Color(0xFF9A7BFF) else Color(0xFF49DDB2),CircleShape))
                        Text(if(sandboxMode)"SANDBOX · ALL PARTS" else "CAREER · "+unlocked.size+" TECH UNLOCKED",fontSize=9.sp,color=muted,modifier=Modifier.padding(start=5.dp),lineHeight=12.sp)
                    }
                }
                TextButton(onClick={showAnalysis=true},contentPadding=PaddingValues(horizontal=7.dp,vertical=6.dp)){Text("ANALYZE",fontSize=10.sp,fontWeight=FontWeight.Black,color=cyan)}
                Button(onClick=onLaunch,enabled=rocket.hasEngine&&rocket.hasCapsule,modifier=Modifier.height(40.dp),shape=RoundedCornerShape(10.dp),contentPadding=PaddingValues(horizontal=12.dp,vertical=5.dp),colors=ButtonDefaults.buttonColors(containerColor=cyan,contentColor=Color(0xFF04111C))){Text("LAUNCH ↗",fontSize=10.sp,fontWeight=FontWeight.Black)}
            }
            Box(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal=7.dp,vertical=6.dp)
                    .background(Color(0xFF0A1928),RoundedCornerShape(16.dp))
                    .border(1.dp,Color(0xFF1B354D),RoundedCornerShape(16.dp))
                    .onGloballyPositioned{viewportBounds=it.boundsInWindow()}
            ){
                RocketStackPreview(
                    rocket,Modifier.fillMaxSize(),onReorder=onMove,verticalPan=canvasPan,maxVisibleParts=10,
                    onPan={delta->
                        val h=viewportBounds?.height?:0f
                        val scale=stackPreviewScale(h,rocket.parts.size,density,10)
                        val total=rocket.parts.size*88f*density*scale+(rocket.parts.size-1).coerceAtLeast(0)*6f*density*scale
                        val limit=max(0f,(total-h)/2f+42f*density)
                        canvasPan=(canvasPan+delta).coerceIn(-limit,limit)
                    }
                )
                Column(Modifier.align(Alignment.TopStart).padding(9.dp).background(Color(0xDD0B1725),RoundedCornerShape(10.dp)).border(1.dp,line,RoundedCornerShape(10.dp)).padding(horizontal=10.dp,vertical=7.dp)){
                    Text("ASSEMBLY",fontSize=9.sp,color=cyan,fontWeight=FontWeight.Black,letterSpacing=1.sp)
                    Text(rocket.parts.size.toString()+" COMPONENTS",fontSize=10.sp,color=ink,fontWeight=FontWeight.Bold)
                    Text((rocket.parts.count{it==PartType.DECOUPLER}+1).toString()+" STAGES",fontSize=9.sp,color=muted)
                }
                Column(Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color(0xDD0B1725),RoundedCornerShape(10.dp)).border(1.dp,line,RoundedCornerShape(10.dp)).padding(horizontal=9.dp,vertical=7.dp),horizontalAlignment=Alignment.End){
                    Text("WET MASS",fontSize=8.sp,color=muted,fontWeight=FontWeight.Bold)
                    Text("%.1f t".format(rocket.dryMass+rocket.fuel),fontSize=13.sp,color=ink,fontWeight=FontWeight.Black)
                    Spacer(Modifier.height(3.dp))
                    Text("TWR",fontSize=8.sp,color=muted,fontWeight=FontWeight.Bold)
                    Text("%.2f".format(stats.twr),fontSize=12.sp,color=if(stats.twr>=1.2)Color(0xFF54E4B5) else Color(0xFFFFBE6A),fontWeight=FontWeight.Black)
                    Text("ΔV "+"%.2f km/s".format(rocket.deltaV),fontSize=10.sp,color=cyan,fontWeight=FontWeight.Black)
                }
                Text("TOP ↑",modifier=Modifier.align(Alignment.TopCenter).padding(top=10.dp).background(Color(0x88203349),RoundedCornerShape(20.dp)).padding(horizontal=10.dp,vertical=4.dp),fontSize=9.sp,color=ink,fontWeight=FontWeight.Black,letterSpacing=.7.sp)
                Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(9.dp).background(Color(0xDD091522),RoundedCornerShape(10.dp)).padding(horizontal=10.dp,vertical=7.dp),verticalAlignment=Alignment.CenterVertically){
                    Text("↕",fontSize=16.sp,color=cyan,fontWeight=FontWeight.Black)
                    Text(if(rocket.parts.isEmpty())"Choose a component below to start building" else "Tap to attach · Drag a part onto the rocket · Drag the stack to reorder",fontSize=9.sp,color=muted,lineHeight=12.sp,modifier=Modifier.padding(start=7.dp).weight(1f))
                    TextButton(onClick={showStack=true},contentPadding=PaddingValues(horizontal=4.dp,vertical=2.dp)){Text("STACK",fontSize=9.sp,fontWeight=FontWeight.Black,color=cyan)}
                }
            }
            Column(Modifier.fillMaxWidth().background(Color(0xFF0A1421)).border(1.dp,Color(0xFF1C3044),RoundedCornerShape(topStart=15.dp,topEnd=15.dp)).padding(top=7.dp,bottom=5.dp)){
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal=8.dp),horizontalArrangement=Arrangement.spacedBy(6.dp),verticalAlignment=Alignment.CenterVertically){
                    categories.forEach{category->
                        val active=selectedCategory==category
                        Surface(color=if(active)Color(0xFF1C3A52) else Color(0xFF101F30),shape=RoundedCornerShape(9.dp),modifier=Modifier.border(1.dp,if(active)Color(0xFF4DBFEF) else Color(0xFF253A50),RoundedCornerShape(9.dp)).clickable{selectedCategory=category}){
                            Text(category,fontSize=9.sp,fontWeight=FontWeight.Black,color=if(active)cyan else muted,modifier=Modifier.padding(horizontal=11.dp,vertical=8.dp))
                        }
                    }
                    Text("TAP OR DRAG",fontSize=8.sp,color=Color(0xFF71869B),fontWeight=FontWeight.Bold)
                }
                Spacer(Modifier.height(6.dp))
                LazyRow(Modifier.fillMaxWidth().height(91.dp).padding(horizontal=7.dp),horizontalArrangement=Arrangement.spacedBy(6.dp),contentPadding=PaddingValues(horizontal=2.dp,vertical=1.dp)){
                    items(parts,key={it.name}){part->
                        val available=isPartUnlocked(part,unlocked)
                        var itemCoordinates by remember(part){mutableStateOf<LayoutCoordinates?>(null)}
                        val dragModifier=if(available)Modifier.pointerInput(part,rocket.parts.size,viewportBounds){
                            detectDragGestures(
                                onDragStart={start->
                                    val origin=itemCoordinates?.localToWindow(Offset.Zero)?:Offset.Zero
                                    draggingPart=part
                                    dragPosition=origin+start
                                },
                                onDrag={change,delta->change.consume();dragPosition=dragPosition?.plus(delta)},
                                onDragEnd={
                                    val position=dragPosition
                                    val bounds=viewportBounds
                                    if(position!=null&&bounds!=null&&position.x>bounds.left&&position.x<bounds.right&&position.y>=bounds.top&&position.y<=bounds.bottom){
                                        val y=(position.y-bounds.top).coerceIn(0f,bounds.height)
                                        onDrop(part,stackInsertionIndex(y,bounds.height,rocket.parts.size,density,canvasPan,10))
                                    }
                                    draggingPart=null
                                    dragPosition=null
                                },
                                onDragCancel={draggingPart=null;dragPosition=null}
                            )
                        }else Modifier
                        Surface(color=if(available)Color(0xFF12263A) else Color(0xFF101822),shape=RoundedCornerShape(11.dp),modifier=Modifier.width(91.dp).fillMaxHeight().onGloballyPositioned{itemCoordinates=it}.then(dragModifier).clickable(enabled=available){onAdd(part)}.border(1.dp,if(available)Color(0xFF294A64) else Color(0xFF202C39),RoundedCornerShape(11.dp))){
                            Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center,modifier=Modifier.fillMaxWidth().padding(horizontal=3.dp,vertical=3.dp)){
                                Box(Modifier.size(43.dp,45.dp),contentAlignment=Alignment.Center){RocketPartThumbnail(part)}
                                Text(if(available)part.title.uppercase() else "LOCKED",fontSize=8.sp,lineHeight=9.sp,maxLines=2,textAlign=androidx.compose.ui.text.style.TextAlign.Center,color=if(available)ink else Color(0xFF728092),fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=1.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal=7.dp),horizontalArrangement=Arrangement.spacedBy(5.dp),verticalAlignment=Alignment.CenterVertically){
                    OutlinedButton(onClick={showStack=true},modifier=Modifier.weight(1f).height(39.dp),contentPadding=PaddingValues(horizontal=3.dp,vertical=5.dp),shape=RoundedCornerShape(8.dp),colors=ButtonDefaults.outlinedButtonColors(contentColor=cyan)){Text("STACK",fontSize=9.sp,fontWeight=FontWeight.Black)}
                    OutlinedButton(onClick={showStageEditor=true},modifier=Modifier.weight(1f).height(39.dp),contentPadding=PaddingValues(horizontal=3.dp,vertical=5.dp),shape=RoundedCornerShape(8.dp),colors=ButtonDefaults.outlinedButtonColors(contentColor=cyan)){Text("STAGES",fontSize=9.sp,fontWeight=FontWeight.Black)}
                    OutlinedButton(onClick={showBlueprintLibrary=true},modifier=Modifier.weight(1.1f).height(39.dp),contentPadding=PaddingValues(horizontal=3.dp,vertical=5.dp),shape=RoundedCornerShape(8.dp),colors=ButtonDefaults.outlinedButtonColors(contentColor=cyan)){Text("DESIGNS",fontSize=9.sp,fontWeight=FontWeight.Black)}
                    OutlinedButton(onClick={showAnalysis=true},modifier=Modifier.weight(1.1f).height(39.dp),contentPadding=PaddingValues(horizontal=3.dp,vertical=5.dp),shape=RoundedCornerShape(8.dp),colors=ButtonDefaults.outlinedButtonColors(contentColor=cyan)){Text("ANALYZE",fontSize=9.sp,fontWeight=FontWeight.Black)}
                    OutlinedButton(onClick=onClear,modifier=Modifier.weight(.8f).height(39.dp),contentPadding=PaddingValues(horizontal=2.dp,vertical=5.dp),shape=RoundedCornerShape(8.dp),colors=ButtonDefaults.outlinedButtonColors(contentColor=Color(0xFFFF8196))){Text("RESET",fontSize=9.sp,fontWeight=FontWeight.Black)}
                }
            }
        }
        if(draggingPart!=null&&dragPosition!=null&&rootBounds!=null){
            val point=dragPosition!!
            val root=rootBounds!!
            Box(Modifier.offset{IntOffset((point.x-root.left-dragHalf).roundToInt(),(point.y-root.top-dragHalf).roundToInt())}.zIndex(5f).width(80.dp).height(84.dp).background(Color(0xF20D2031),RoundedCornerShape(12.dp)).border(2.dp,cyan,RoundedCornerShape(12.dp)),contentAlignment=Alignment.Center){
                Column(horizontalAlignment=Alignment.CenterHorizontally){
                    RocketPartThumbnail(draggingPart!!)
                    Text(draggingPart!!.title,fontSize=8.sp,lineHeight=9.sp,maxLines=2,color=ink,fontWeight=FontWeight.Bold,textAlign=androidx.compose.ui.text.style.TextAlign.Center,modifier=Modifier.padding(horizontal=4.dp))
                }
            }
        }
        if(showBlueprintLibrary)BlueprintLibraryDialog(rocket=rocket,blueprints=blueprints,onSave=onSaveBlueprint,onLoad={name->onLoadBlueprint(name);showBlueprintLibrary=false},onDelete=onDeleteBlueprint,onClose={showBlueprintLibrary=false})
        if(showStageEditor)StageEditorDialog(rocket=rocket,onMove=onMove,onRemove=onRemove,onAddStage=onAddStage,onClose={showStageEditor=false})
        if(showStack)AlertDialog(
            onDismissRequest={showStack=false},title={Text("ROCKET STACK",fontWeight=FontWeight.Black,color=ink)},
            text={
                if(rocket.parts.isEmpty())Text("Your vehicle is empty. Tap a component below to begin.")
                else LazyColumn(verticalArrangement=Arrangement.spacedBy(5.dp),modifier=Modifier.heightIn(max=420.dp)){
                    itemsIndexed(rocket.parts){index,part->
                        Row(Modifier.fillMaxWidth().background(Color(0xFF132638),RoundedCornerShape(9.dp)).padding(horizontal=7.dp,vertical=5.dp),verticalAlignment=Alignment.CenterVertically){
                            RocketPartThumbnail(part)
                            Column(Modifier.weight(1f).padding(start=7.dp)){
                                Text(part.title,fontSize=12.sp,fontWeight=FontWeight.Bold,color=ink,lineHeight=14.sp)
                                Text("M %.1f t · F %.1f t".format(part.mass,part.fuel),fontSize=10.sp,color=muted)
                            }
                            Column(horizontalAlignment=Alignment.CenterHorizontally){
                                Text("↑",fontSize=17.sp,color=cyan,modifier=Modifier.clickable{onMove(index,-1)}.padding(horizontal=6.dp))
                                Text("↓",fontSize=17.sp,color=cyan,modifier=Modifier.clickable{onMove(index,1)}.padding(horizontal=6.dp))
                            }
                            Text("×",fontSize=20.sp,color=Color(0xFFFF8196),modifier=Modifier.clickable{onRemove(index)}.padding(horizontal=6.dp))
                        }
                    }
                }
            },confirmButton={TextButton(onClick={showStack=false},colors=ButtonDefaults.textButtonColors(contentColor=cyan)){Text("DONE")}},containerColor=Color(0xFF0E1B2A)
        )
        if(showAnalysis)AlertDialog(
            onDismissRequest={showAnalysis=false},title={Text("VEHICLE ANALYSIS",fontWeight=FontWeight.Black,color=ink)},
            text={
                Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(8.dp)){
                    Row(horizontalArrangement=Arrangement.spacedBy(7.dp),modifier=Modifier.fillMaxWidth()){
                        Metric("WET MASS","%.1f t".format(stats.totalMass),Modifier.weight(1f))
                        Metric("FUEL","%.1f t".format(stats.fuelMass),Modifier.weight(1f))
                        Metric("THRUST","%.2f MN".format(stats.thrust/1000000.0),Modifier.weight(1f))
                    }
                    Row(horizontalArrangement=Arrangement.spacedBy(7.dp),modifier=Modifier.fillMaxWidth()){
                        Metric("TWR","%.2f".format(stats.twr),Modifier.weight(1f))
                        Metric("DELTA-V","%.2f km/s".format(rocket.deltaV),Modifier.weight(1f))
                    }
                    Text("ESTIMATED ALTITUDE  "+stats.estimatedMaxAltitudeKm+" km",fontSize=12.sp,color=cyan,fontWeight=FontWeight.Black)
                    Text("Performance is approximate and depends on stage separation, thrust and propellant.",fontSize=11.sp,color=muted,lineHeight=15.sp)
                    if(stats.warnings.isEmpty())Text("✓ VEHICLE CONFIGURATION LOOKS GOOD",fontSize=11.sp,color=Color(0xFF54E4B5),fontWeight=FontWeight.Black)
                    else stats.warnings.forEach{warning->Row(verticalAlignment=Alignment.Top){Text("!",fontSize=13.sp,color=Color(0xFFFFBE6A),fontWeight=FontWeight.Black);Text(warning,fontSize=11.sp,color=muted,modifier=Modifier.padding(start=7.dp))}}
                }
            },confirmButton={TextButton(onClick={showAnalysis=false},colors=ButtonDefaults.textButtonColors(contentColor=cyan)){Text("CLOSE",fontWeight=FontWeight.Black)}},containerColor=Color(0xFF0E1B2A)
        )
    }
}
@Composable
private fun BlueprintLibraryDialog(
    rocket:Rocket,
    blueprints:Map<String,Rocket>,
    onSave:(String)->Unit,
    onLoad:(String)->Unit,
    onDelete:(String)->Unit,
    onClose:()->Unit
){
    var name by rememberSaveable{mutableStateOf("")}
    AlertDialog(
        onDismissRequest=onClose,
        title={Text("BLUEPRINT LIBRARY",fontWeight=FontWeight.Black,color=Color(0xFFEAF3FC))},
        text={
            Column(Modifier.fillMaxWidth()){
                Text("Save the current assembly by name, then load it into the builder whenever you need it. Saving over an existing name replaces that design.",fontSize=12.sp,color=Color(0xFF9DB2C7),lineHeight=16.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value=name,
                    onValueChange={value->name=value.filter{it.isLetterOrDigit()||it==' '||it=='-'||it=='_'}.take(32)},
                    modifier=Modifier.fillMaxWidth(),
                    label={Text("Design name")},
                    singleLine=true
                )
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick={onSave(name.trim());name=""},
                    enabled=name.trim().isNotEmpty()&&rocket.parts.isNotEmpty(),
                    modifier=Modifier.fillMaxWidth(),
                    colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF45D7FF),contentColor=Color(0xFF061321))
                ){Text("SAVE CURRENT DESIGN",fontWeight=FontWeight.Black)}
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF2B4A63)))
                Spacer(Modifier.height(6.dp))
                if(blueprints.isEmpty()){
                    Text("No saved designs yet.",fontSize=12.sp,color=Color(0xFF8DA6BC),modifier=Modifier.padding(vertical=8.dp))
                }else{
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max=250.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                        items(blueprints.keys.sorted(),key={it}){blueprintName->
                            val blueprint=blueprints.getValue(blueprintName)
                            Row(
                                Modifier.fillMaxWidth().background(Color(0xFF132638),RoundedCornerShape(7.dp)).padding(horizontal=8.dp,vertical=6.dp),
                                verticalAlignment=Alignment.CenterVertically
                            ){
                                Column(Modifier.weight(1f)){
                                    Text(blueprintName,fontSize=12.sp,fontWeight=FontWeight.Bold,color=Color(0xFFEAF3FC),maxLines=1)
                                    Text("${blueprint.parts.size} parts · ${blueprint.parts.count{it==PartType.DECOUPLER}+1} stages",fontSize=10.sp,color=Color(0xFF8DA6BC))
                                }
                                TextButton(onClick={onLoad(blueprintName)},contentPadding=PaddingValues(horizontal=5.dp,vertical=2.dp)){
                                    Text("LOAD",fontSize=10.sp,fontWeight=FontWeight.Black,color=Color(0xFF45D7FF))
                                }
                                TextButton(onClick={onDelete(blueprintName)},contentPadding=PaddingValues(horizontal=5.dp,vertical=2.dp)){
                                    Text("×",fontSize=18.sp,fontWeight=FontWeight.Black,color=Color(0xFFFF7189))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton={TextButton(onClick=onClose,colors=ButtonDefaults.textButtonColors(contentColor=Color(0xFF45D7FF))){Text("DONE",fontWeight=FontWeight.Black)}},
        containerColor=Color(0xFF0E1B2A)
    )
}

@Composable
private fun StageEditorDialog(
    rocket:Rocket,
    onMove:(Int,Int)->Unit,
    onRemove:(Int)->Unit,
    onAddStage:()->Unit,
    onClose:()->Unit
){
    val stages=remember(rocket.parts){
        val ranges=mutableListOf<IntRange>()
        var start=0
        rocket.parts.forEachIndexed{index,part->
            if(part==PartType.DECOUPLER){
                ranges.add(start until index)
                start=index+1
            }
        }
        ranges.add(start until rocket.parts.size)
        ranges
    }
    AlertDialog(
        onDismissRequest=onClose,
        title={Text("STAGE EDITOR",fontWeight=FontWeight.Black,color=Color(0xFFEAF3FC))},
        text={
            Column(Modifier.fillMaxWidth()){
                Text("Stages ignite from the bottom up. Reorder parts within a stage; remove a separator to merge stages.",fontSize=12.sp,color=Color(0xFF9DB2C7),lineHeight=16.sp)
                Spacer(Modifier.height(10.dp))
                LazyColumn(
                    modifier=Modifier.fillMaxWidth().heightIn(max=420.dp),
                    verticalArrangement=Arrangement.spacedBy(8.dp)
                ){
                    items(stages.indices.reversed().toList(),key={it}){stageIndex->
                        val range=stages[stageIndex]
                        val stageNumber=stages.size-stageIndex
                        Column(verticalArrangement=Arrangement.spacedBy(5.dp)){
                            Surface(
                                color=if(stageNumber==1)Color(0xFF15364A) else Color(0xFF132638),
                                shape=RoundedCornerShape(9.dp),
                                modifier=Modifier.fillMaxWidth()
                            ){
                                Column(Modifier.padding(9.dp)){
                                    Row(verticalAlignment=Alignment.CenterVertically){
                                        Text("STAGE $stageNumber",fontSize=12.sp,fontWeight=FontWeight.Black,color=Color(0xFFEAF3FC),modifier=Modifier.weight(1f))
                                        if(stageNumber==1)Text("ACTIVE",fontSize=9.sp,fontWeight=FontWeight.Black,color=Color(0xFF45E0A8))
                                        else Text("UPPER",fontSize=9.sp,fontWeight=FontWeight.Bold,color=Color(0xFF8DA6BC))
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    if(range.isEmpty()){
                                        Text("No parts in this stage. Add a component in the builder.",fontSize=11.sp,color=Color(0xFF8DA6BC))
                                    }else{
                                        range.forEach{index->
                                            val part=rocket.parts[index]
                                            Row(
                                                modifier=Modifier.fillMaxWidth().padding(vertical=2.dp).background(Color(0xFF0D1C2B),RoundedCornerShape(6.dp)).padding(horizontal=7.dp,vertical=5.dp),
                                                verticalAlignment=Alignment.CenterVertically
                                            ){
                                                RocketPartThumbnail(part)
                                                Text(part.title,fontSize=11.sp,fontWeight=FontWeight.SemiBold,color=Color(0xFFEAF3FC),modifier=Modifier.weight(1f).padding(start=7.dp))
                                                Text("↑",fontSize=18.sp,color=if(index>range.first)Color(0xFF45D7FF) else Color(0xFF405366),modifier=Modifier.clickable(enabled=index>range.first){onMove(index,-1)}.padding(horizontal=7.dp))
                                                Text("↓",fontSize=18.sp,color=if(index<range.last)Color(0xFF45D7FF) else Color(0xFF405366),modifier=Modifier.clickable(enabled=index<range.last){onMove(index,1)}.padding(horizontal=7.dp))
                                                Text("×",fontSize=19.sp,color=Color(0xFFFF7189),modifier=Modifier.clickable{onRemove(index)}.padding(start=7.dp))
                                            }
                                        }
                                    }
                                }
                            }
                            if(stageIndex>0){
                                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                                    Box(Modifier.weight(1f).height(1.dp).background(Color(0xFF2B4A63)))
                                    Text(" SEPARATOR ",fontSize=9.sp,fontWeight=FontWeight.Black,color=Color(0xFFFFB74D),modifier=Modifier.padding(horizontal=6.dp))
                                    Box(Modifier.weight(1f).height(1.dp).background(Color(0xFF2B4A63)))
                                    TextButton(onClick={onRemove(range.first-1)},contentPadding=PaddingValues(horizontal=6.dp,vertical=1.dp)){
                                        Text("REMOVE",fontSize=9.sp,color=Color(0xFFFF7189),fontWeight=FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton={
            TextButton(onClick=onClose,colors=ButtonDefaults.textButtonColors(contentColor=Color(0xFF45D7FF))){
                Text("DONE",fontWeight=FontWeight.Black)
            }
        },
        dismissButton={
            TextButton(onClick=onAddStage,colors=ButtonDefaults.textButtonColors(contentColor=Color(0xFF45D7FF))){
                Text("+ ADD UPPER STAGE",fontWeight=FontWeight.Black,fontSize=10.sp)
            }
        },
        containerColor=Color(0xFF0E1B2A)
    )
}


@Composable
private fun TutorialScreen(onFinish:()->Unit,onSkip:()->Unit){
    var page by rememberSaveable{mutableIntStateOf(0)}
    val lessons=listOf(
        Triple("01  /  BUILD","Choose a component category at the bottom of the workshop. Tap a part to attach it, or drag it onto the rocket to choose where it goes.","Installed parts can be reordered by dragging the rocket stack, or precisely edited in STACK and STAGES."),
        Triple("02  /  LAUNCH","Build a capsule, fuel tanks and an engine, then press LAUNCH. STAGE separates the lower section when its fuel is spent.","Sandbox unlocks every component. Career keeps the research tree and contracts available alongside it."),
        Triple("03  /  REACH ORBIT","Climb through the atmosphere, then gradually tilt toward the horizon. Open MAP to inspect your path and your orbital trajectory.","A stable orbit needs both periapsis and apoapsis above the atmosphere. Return here whenever you need a reminder.")
    )
    val lesson=lessons[page]
    val ink=Color(0xFFEAF2FB)
    val muted=Color(0xFF91A4B9)
    val accent=Color(0xFF66B9E9)
    Column(Modifier.fillMaxSize().background(Color(0xFF070C14)).padding(horizontal=16.dp,vertical=13.dp)){
        Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.fillMaxWidth()){
            Column(Modifier.weight(1f)){
                Text("FLIGHT SCHOOL",fontSize=10.sp,color=accent,fontWeight=FontWeight.Black,letterSpacing=1.4.sp)
                Text("Learn by launching",fontSize=24.sp,fontWeight=FontWeight.Black,color=ink)
            }
            Text((page+1).toString()+" / "+lessons.size,fontSize=12.sp,color=muted,fontWeight=FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        Surface(color=Color(0xFF101A28),shape=RoundedCornerShape(15.dp),modifier=Modifier.fillMaxWidth().weight(1f).border(1.dp,Color(0xFF263B50),RoundedCornerShape(15.dp))){
            Column(Modifier.padding(15.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                Box(Modifier.fillMaxWidth().height(180.dp).background(Color(0xFF0A1928),RoundedCornerShape(11.dp)).border(1.dp,Color(0xFF29445D),RoundedCornerShape(11.dp))){
                    RocketStackPreview(Rocket(listOf(PartType.NOSE,PartType.CAPSULE,PartType.HEATSHIELD,PartType.TANK,PartType.TANK,PartType.VACUUM_ENGINE,PartType.FIN)),Modifier.fillMaxSize())
                    Text("ORVITARY  /  FIELD GUIDE",modifier=Modifier.align(Alignment.TopStart).padding(10.dp).background(Color(0xDD091522),RoundedCornerShape(6.dp)).padding(horizontal=8.dp,vertical=5.dp),fontSize=8.sp,color=accent,fontWeight=FontWeight.Black,letterSpacing=.7.sp)
                }
                Text(lesson.first,fontSize=10.sp,color=accent,fontWeight=FontWeight.Black,letterSpacing=1.sp)
                Text(lesson.second,fontSize=16.sp,lineHeight=22.sp,fontWeight=FontWeight.SemiBold,color=ink)
                Text(lesson.third,fontSize=12.sp,lineHeight=17.sp,color=muted)
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement=Arrangement.spacedBy(5.dp),modifier=Modifier.fillMaxWidth()){
                    lessons.indices.forEach{idx->Box(Modifier.weight(1f).height(4.dp).background(if(idx<=page)accent else Color(0xFF29394B),RoundedCornerShape(4.dp)))}
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(7.dp),modifier=Modifier.fillMaxWidth()){
            OutlinedButton(onClick=onSkip,modifier=Modifier.weight(1f).height(46.dp),shape=RoundedCornerShape(9.dp),colors=ButtonDefaults.outlinedButtonColors(contentColor=ink)){Text("SKIP",fontWeight=FontWeight.Bold)}
            if(page>0)OutlinedButton(onClick={page--},modifier=Modifier.weight(1f).height(46.dp),shape=RoundedCornerShape(9.dp),colors=ButtonDefaults.outlinedButtonColors(contentColor=ink)){Text("BACK",fontWeight=FontWeight.Bold,fontSize=10.sp)}
            Button(onClick={if(page==lessons.lastIndex)onFinish() else page++},modifier=Modifier.weight(1.4f).height(46.dp),shape=RoundedCornerShape(9.dp),colors=ButtonDefaults.buttonColors(containerColor=accent,contentColor=Color(0xFF061522))){Text(if(page==lessons.lastIndex)"START PLAYING" else "CONTINUE",fontWeight=FontWeight.Black,fontSize=10.sp)}
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
