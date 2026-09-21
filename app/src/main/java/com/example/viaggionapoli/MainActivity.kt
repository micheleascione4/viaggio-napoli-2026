package com.example.viaggionapoli

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Navy = Color(0xFF0B2237)
private val Blue = Color(0xFF247DB8)
private val Sky = Color(0xFFEAF4FA)
private val Sand = Color(0xFFF4E8D5)
private val Green = Color(0xFFE5F5F1)
private val Ink = Color(0xFF16222D)
private val Muted = Color(0xFF667788)

data class Day(val date:String,val title:String,val subtitle:String,val tag:String,val accent:Color)
data class Place(val name:String,val detail:String,val kind:String)

val days = listOf(
    Day("21 SET","Arrivo + Chiaia","Cervia → Pompei · prima immersione a Napoli","ARRIVO",Blue),
    Day("22 SET","Napoli centro + shopping","Centro storico · giacca + camicia","NAPOLI",Blue),
    Day("23 SET","Giornata di mare","Una sola giornata mare · scelta in base al meteo","MARE",Color(0xFF159A88)),
    Day("24 SET","Parenti + San Carlo","Caffè dai parenti · Mitridate alle 19:00","FISSO",Color(0xFF9A4D58)),
    Day("25 SET","Pompei Scavi","Giornata archeologica","POMPEI",Sand),
    Day("26 SET","Sotterranea + Fontanelle","Partenza da Gambrinus · Sanità","NAPOLI",Blue),
    Day("27 SET","Barra · serata finale","Festa dei Gigli · seconda parte dalle 18:00","GIGLI",Sand),
    Day("28 SET","Check-out + rientro","Mattina libera · rientro","RIENTRO",Color(0xFF708090))
)

val mealData = mapOf(
  21 to Pair(
    listOf(
      Place("Cuccuma Caffè","Pompei · colazione/pranzo rapido","street food"),
      Place("Pizza a portafoglio","Napoli centro · soluzione veloce","street food"),
      Place("Antica Pizzeria Di Matteo","Via dei Tribunali · pizza","street food"),
      Place("L'Antica Pizzeria da Michele","Centro · pizza","street food"),
      Place("Pizzeria Starita","Materdei · pizza","street food"),
      Place("Pizzeria Pellone","Napoli · pizza","street food"),
      Place("Mazz Bar","Chiaia · pausa informale","street food"),
      Place("Tarallificio Leopoldo","Napoli · snack campani","street food")
    ),
    listOf(
      Place("Cozzolino Braceria","Barra · carne","ristorante"),
      Place("Da Alfredo","Via Traccia · cucina campana","ristorante"),
      Place("Ermenegildo","Barra · pizzeria con servizio","ristorante"),
      Place("Castello Showbiz","Via Luigi Volpicella · ristorante","ristorante"),
      Place("Mimì alla Ferrovia","Napoli · cucina napoletana","ristorante"),
      Place("Tandem","Napoli · cucina tradizionale","ristorante"),
      Place("Trattoria Castel dell'Ovo","Napoli · cucina campana","ristorante"),
      Place("Osteria della Mattonella","Chiaia · cucina napoletana","ristorante")
    )
  ),
  22 to Pair(
    listOf(
      Place("Antica Pizzeria Di Matteo","Tribunali · pizza","street food"),
      Place("L'Antica Pizzeria da Michele","Forcella · pizza","street food"),
      Place("Gino e Toto Sorbillo","Via dei Tribunali · pizza","street food"),
      Place("Pizzeria Dal Presidente","Tribunali · pizza","street food"),
      Place("Cuccuma Caffè","Centro · pausa veloce","street food"),
      Place("Sfogliatella Mary","Galleria Umberto · dolce","street food"),
      Place("La Masardona","Pizza fritta · centro","street food"),
      Place("Tarallificio Leopoldo","Centro · snack campani","street food")
    ),
    listOf(
      Place("Trattoria Castel dell'Ovo","Lungomare · cucina campana","ristorante"),
      Place("Osteria della Mattonella","Chiaia · cucina napoletana","ristorante"),
      Place("Tandem","Centro · tradizione napoletana","ristorante"),
      Place("Mimì alla Ferrovia","Centrale · cucina campana","ristorante"),
      Place("Ristorante Umberto","Chiaia · cucina italiana","ristorante"),
      Place("La Cantina del Gallo Nero","Centro · cucina campana","ristorante"),
      Place("Mattozzi","Piazza Carità · cucina napoletana","ristorante"),
      Place("Antica Capri","Quartieri Spagnoli · cucina campana","ristorante")
    )
  ),
  23 to Pair(
    listOf(
      Place("A Puteca e Masaniello","Sorrento · pausa veloce","street food"),
      Place("La Sfizieria","Sorrento · snack","street food"),
      Place("Da Gigino","Sorrento · pizza","street food"),
      Place("L'Antica Trattoria da Carmine","Sorrento · pranzo rapido","street food"),
      Place("Delizia al Limone","Sorrento · dolce","street food"),
      Place("Pizza al taglio","Positano · soluzione veloce","street food"),
      Place("Fornillo Snack Bar","Positano · snack","street food"),
      Place("Cuoppo di mare","Costiera · fritto da passeggio","street food")
    ),
    listOf(
      Place("Trattoria da Gigino","Sorrento · cucina campana","ristorante"),
      Place("O'Parrucchiano","Sorrento · cucina italiana","ristorante"),
      Place("Ristorante Il Buco","Sorrento · cucina italiana","ristorante"),
      Place("La Tagliata","Positano · cucina campana","ristorante"),
      Place("Chez Black","Positano · cucina di mare","ristorante"),
      Place("Da Vincenzo","Positano · cucina campana","ristorante"),
      Place("Ristorante Bruno","Positano · cucina italiana","ristorante"),
      Place("Marina Grande","Sorrento · cucina di mare","ristorante")
    )
  ),
  24 to Pair(
    listOf(
      Place("Pasticceria Poppella","Napoli · dolce","street food"),
      Place("Sfogliatella Mary","Plebiscito · dolce","street food"),
      Place("Cuccuma Caffè","Centro · pausa veloce","street food"),
      Place("Tarallificio Leopoldo","Centro · snack","street food"),
      Place("La Masardona","Pizza fritta · pranzo","street food"),
      Place("Di Matteo","Tribunali · pizza","street food"),
      Place("Sorbillo","Tribunali · pizza","street food"),
      Place("Pizzeria Dal Presidente","Tribunali · pizza","street food")
    ),
    listOf(
      Place("Ristorante Umberto","Chiaia · cucina italiana","ristorante"),
      Place("Mimì alla Ferrovia","Napoli · cucina campana","ristorante"),
      Place("Tandem","Centro · cucina tradizionale","ristorante"),
      Place("Mattozzi","Centro · cucina napoletana","ristorante"),
      Place("Antica Capri","Quartieri Spagnoli · cucina campana","ristorante"),
      Place("Trattoria Castel dell'Ovo","Lungomare · cucina campana","ristorante"),
      Place("Osteria della Mattonella","Chiaia · cucina napoletana","ristorante"),
      Place("La Cantina del Gallo Nero","Centro · cucina campana","ristorante")
    )
  ),
  25 to Pair(
    listOf(
      Place("Pizzeria da Franco","Pompei · pizza","street food"),
      Place("Na Pasta","Pompei · pasta veloce","street food"),
      Place("Pizza a portafoglio","Pompei · snack","street food"),
      Place("Panino campano","Pompei · pranzo rapido","street food"),
      Place("Cuoppo","Pompei · fritto","street food"),
      Place("Gelateria Punto G","Pompei · pausa","street food"),
      Place("Pasticceria De Vivo","Pompei · dolce","street food"),
      Place("Street food di via Sacra","Pompei · soluzione veloce","street food")
    ),
    listOf(
      Place("President","Pompei · cucina italiana","ristorante"),
      Place("Il Principe","Pompei · cucina campana","ristorante"),
      Place("Le Arcate","Pompei · cucina italiana","ristorante"),
      Place("La Bettola del Gusto","Pompei · cucina campana","ristorante"),
      Place("Ristorante Garum","Pompei · cucina mediterranea","ristorante"),
      Place("Stella","Pompei · cucina italiana","ristorante"),
      Place("Caupona","Pompei · cucina campana","ristorante"),
      Place("Varnelli","Pompei · cucina italiana","ristorante")
    )
  ),
  26 to Pair(
    listOf(
      Place("Di Matteo","Tribunali · pizza","street food"),
      Place("Sorbillo","Tribunali · pizza","street food"),
      Place("La Masardona","Centro · pizza fritta","street food"),
      Place("Pizzeria Dal Presidente","Tribunali · pizza","street food"),
      Place("Poppella","Sanità · dolce","street food"),
      Place("Concettina ai Tre Santi","Sanità · pizza","street food"),
      Place("Tarallificio Leopoldo","Centro · snack","street food"),
      Place("Cuoppo napoletano","Centro · fritto","street food")
    ),
    listOf(
      Place("Concettina ai Tre Santi","Sanità · pizzeria con servizio","ristorante"),
      Place("Starita","Materdei · pizza e cucina","ristorante"),
      Place("Tandem","Centro · cucina napoletana","ristorante"),
      Place("Mimì alla Ferrovia","Napoli · cucina campana","ristorante"),
      Place("Osteria della Mattonella","Chiaia · cucina napoletana","ristorante"),
      Place("Mattozzi","Centro · cucina campana","ristorante"),
      Place("Antica Capri","Quartieri Spagnoli · cucina campana","ristorante"),
      Place("Ristorante Umberto","Chiaia · cucina italiana","ristorante")
    )
  ),
  27 to Pair(
    listOf(
      Place("Pizza a portafoglio","Napoli · pranzo veloce","street food"),
      Place("La Masardona","Pizza fritta · pranzo","street food"),
      Place("Di Matteo","Tribunali · pizza","street food"),
      Place("Sorbillo","Tribunali · pizza","street food"),
      Place("Cuoppo di mare","Napoli · fritto","street food"),
      Place("Poppella","Napoli · dolce","street food"),
      Place("Tarallificio Leopoldo","Napoli · snack","street food"),
      Place("Panino napoletano","Napoli · snack","street food")
    ),
    listOf(
      Place("Cozzolino Braceria","Barra · braceria","ristorante"),
      Place("Da Alfredo","Via Traccia · cucina campana","ristorante"),
      Place("Ermenegildo","Barra · pizzeria con servizio","ristorante"),
      Place("Castello Showbiz","Via Luigi Volpicella · ristorante","ristorante"),
      Place("Mimì alla Ferrovia","Napoli · cucina campana","ristorante"),
      Place("Tandem","Napoli · cucina tradizionale","ristorante"),
      Place("Mattozzi","Napoli · cucina napoletana","ristorante"),
      Place("Osteria della Mattonella","Chiaia · cucina napoletana","ristorante")
    )
  ),
  28 to Pair(
    listOf(
      Place("Pasticceria De Vivo","Pompei · colazione","street food"),
      Place("Cuccuma Caffè","Pompei · colazione","street food"),
      Place("Pasticceria Poppella","Napoli · dolce","street food"),
      Place("Sfogliatella Mary","Napoli · dolce","street food"),
      Place("Tarallificio Leopoldo","Napoli · snack","street food"),
      Place("Di Matteo","Napoli · pranzo","street food"),
      Place("La Masardona","Napoli · pizza fritta","street food"),
      Place("Pizza a portafoglio","Napoli · soluzione rapida","street food")
    ),
    listOf(
      Place("Ristorante Garum","Pompei · cucina mediterranea","ristorante"),
      Place("President","Pompei · cucina italiana","ristorante"),
      Place("Caupona","Pompei · cucina campana","ristorante"),
      Place("La Bettola del Gusto","Pompei · cucina campana","ristorante"),
      Place("Le Arcate","Pompei · cucina italiana","ristorante"),
      Place("Stella","Pompei · cucina italiana","ristorante"),
      Place("Il Principe","Pompei · cucina campana","ristorante"),
      Place("Varnelli","Pompei · cucina italiana","ristorante")
    )
  )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { App() }
    }
}

@Composable
fun App() {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Navy, secondary = Blue, background = Color(0xFFF4F7FA),
            surface = Color.White, onSurface = Ink
        ),
        typography = Typography(
            headlineLarge = LocalTextStyle.current.copy(fontSize = 30.sp, fontWeight = FontWeight.Black),
            titleLarge = LocalTextStyle.current.copy(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        )
    ) {
        var selected by rememberSaveable { mutableIntStateOf(0) }
        var tab by rememberSaveable { mutableIntStateOf(0) }
        Scaffold(
            containerColor = Color(0xFFF4F7FA),
            topBar = { AppTopBar() },
            bottomBar = { BottomBar(tab) { tab = it } }
        ) { pad ->
            when(tab) {
                0 -> Home(Modifier.padding(pad), selected) { selected = it }
                1 -> DayDetail(Modifier.padding(pad), selected)
                2 -> Weather(Modifier.padding(pad))
                else -> Dining(Modifier.padding(pad), selected)
            }
        }
    }
}

@Composable
fun AppTopBar() {
    Surface(color = Navy) {
        Column(Modifier.fillMaxWidth().padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())) {
            Row(Modifier.padding(horizontal=18.dp,vertical=13.dp), verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("VIAGGIO NAPOLI", color=Color(0xFFAED6EA), fontSize=10.sp, fontWeight=FontWeight.Bold, letterSpacing=1.5.sp)
                    Text("21—28 SET 2026", color=Color.White, fontSize=23.sp, fontWeight=FontWeight.Black)
                }
                Surface(shape=RoundedCornerShape(50),color=Color.White.copy(alpha=.10f)) {
                    Text("v3.0 · NATIVE",color=Color.White,fontSize=9.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(9.dp))
                }
            }
        }
    }
}

@Composable
fun Home(modifier:Modifier,selected:Int,onSelect:(Int)->Unit) {
    LazyColumn(modifier.fillMaxSize(), contentPadding=PaddingValues(16.dp,16.dp,16.dp,110.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors=CardDefaults.cardColors(containerColor=Color.White),shape=RoundedCornerShape(26.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text("Il viaggio, senza attrito.",fontSize=28.sp,fontWeight=FontWeight.Black,color=Navy)
                    Spacer(Modifier.height(6.dp))
                    Text("Programma, pasti, meteo e spostamenti sono organizzati per giornata. Layout nativo Android, pensato per S23 e display stretti.",color=Muted,fontSize=13.sp)
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        Metric("1","giornata mare"); Metric("19:00","San Carlo"); Metric("18:00+","Gigli")
                    }
                }
            }
        }
        item { Text("ITINERARIO",fontSize=11.sp,fontWeight=FontWeight.Black,color=Muted,letterSpacing=1.3.sp) }
        items(days.indices.toList()) { i ->
            DayCard(days[i],i==selected) { onSelect(i) }
        }
    }
}

@Composable
fun Metric(a:String,b:String) {
    Surface(shape=RoundedCornerShape(14.dp),color=Sky,modifier=Modifier.weight(1f)) {
        Column(Modifier.padding(10.dp)) { Text(a,fontWeight=FontWeight.Black,fontSize=17.sp,color=Navy); Text(b,fontSize=9.sp,color=Muted) }
    }
}

@Composable
fun DayCard(day:Day,active:Boolean,onClick:()->Unit) {
    Card(onClick=onClick,shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color.White),
        border=if(active) androidx.compose.foundation.BorderStroke(2.dp,day.accent) else null) {
        Row(Modifier.padding(15.dp),verticalAlignment=Alignment.CenterVertically) {
            Surface(shape=RoundedCornerShape(14.dp),color=day.accent.copy(alpha=.14f)) {
                Text(day.date,Modifier.padding(horizontal=10.dp,vertical=9.dp),fontSize=10.sp,fontWeight=FontWeight.Black,color=Navy)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(day.title,fontSize=16.sp,fontWeight=FontWeight.ExtraBold)
                Text(day.subtitle,fontSize=11.sp,color=Muted,maxLines=2,overflow=TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.ChevronRight,null,tint=Muted)
        }
    }
}

@Composable
fun DayDetail(modifier:Modifier,selected:Int) {
    val d=days[selected]
    val events = when(selected) {
        0 -> listOf("11:00 · Partenza da Cervia","13:00–14:00 · Pausa ricarica","18:00–20:00 · Chiaia","20:00–21:15 · Chiaia → Pompei","21:15+ · Arrivo + cena")
        1 -> listOf("Mattina · Napoli centro","11:30 · Giacca + camicia","Pomeriggio · Spaccanapoli / Tribunali","Sera · cena in ristorante")
        2 -> listOf("Mattina · partenza per il mare","Giornata · mare e relax","Sera · rientro")
        3 -> listOf("Mattina · caffè dai parenti","16:00 · trasferimento verso San Carlo","19:00 · Mitridate, re di Ponto","Dopo · rientro")
        4 -> listOf("Mattina · Pompei Scavi","Pranzo · pausa","Pomeriggio · seconda parte / rientro")
        5 -> listOf("Mattina · Napoli centro","Slot prenotato · Napoli Sotterranea da Gambrinus","Pomeriggio · Sanità","Pomeriggio/sera · Cimitero delle Fontanelle")
        6 -> listOf("Pomeriggio · arrivo verso Barra","17:00 · orientamento in zona","18:00+ · seconda parte della Ballata","Sera · rientro")
        else -> listOf("Mattina · check-out","Giornata · rientro")
    }
    LazyColumn(modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp,16.dp,16.dp,110.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        item { HeaderDay(d) }
        items(events) { EventRow(it) }
        item { Spacer(Modifier.height(6.dp)); DiningPreview(selected+21) }
    }
}

@Composable
fun HeaderDay(d:Day) {
    Card(shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=Color.White)) {
        Column(Modifier.padding(20.dp)) {
            Text(d.date,color=Blue,fontSize=10.sp,fontWeight=FontWeight.Black,letterSpacing=1.3.sp)
            Text(d.title,fontSize=27.sp,fontWeight=FontWeight.Black,color=Navy)
            Text(d.subtitle,color=Muted,fontSize=12.sp)
        }
    }
}

@Composable
fun EventRow(s:String) {
    Row(Modifier.fillMaxWidth().padding(vertical=4.dp),verticalAlignment=Alignment.CenterVertically) {
        Surface(shape=RoundedCornerShape(50),color=Blue,modifier=Modifier.size(9.dp)) {}
        Spacer(Modifier.width(12.dp))
        Text(s,fontSize=13.sp,fontWeight=FontWeight.SemiBold)
    }
}

@Composable
fun DiningPreview(day:Int) {
    val data=mealData[day] ?: return
    Column {
        Text("DOVE MANGIARE",fontSize=11.sp,fontWeight=FontWeight.Black,color=Muted,letterSpacing=1.3.sp)
        Spacer(Modifier.height(8.dp))
        MealSection("PRANZO · STREET FOOD + VELOCE",data.first,Sky)
        Spacer(Modifier.height(10.dp))
        MealSection("SERA · SOLO RISTORANTI",data.second,Sand)
    }
}

@Composable
fun MealSection(title:String,places:List<Place>,bg:Color) {
    Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color.White)) {
        Column(Modifier.padding(14.dp)) {
            Surface(shape=RoundedCornerShape(10.dp),color=bg) {
                Text(title,Modifier.padding(8.dp),fontSize=10.sp,fontWeight=FontWeight.Black,color=Navy)
            }
            Spacer(Modifier.height(8.dp))
            places.forEachIndexed { i,p ->
                PlaceRow(p,i+1)
                if(i<places.lastIndex) HorizontalDivider(color=Color(0xFFE8EDF1),modifier=Modifier.padding(vertical=6.dp))
            }
        }
    }
}

@Composable
fun PlaceRow(p:Place,n:Int) {
    val context=LocalContext.current
    Row(Modifier.fillMaxWidth().padding(vertical=4.dp),verticalAlignment=Alignment.CenterVertically) {
        Text(String.format("%02d",n),fontSize=10.sp,fontWeight=FontWeight.Black,color=Muted,modifier=Modifier.width(28.dp))
        Column(Modifier.weight(1f)) {
            Text(p.name,fontSize=12.sp,fontWeight=FontWeight.Bold)
            Text(p.detail,fontSize=10.sp,color=Muted)
        }
        IconButton(onClick={
            val q=Uri.encode(p.name+" "+p.detail)
            context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/maps/search/?api=1&query=$q")))
        }) { Icon(Icons.Default.Place,"Maps",tint=Blue) }
    }
}

@Composable
fun Dining(modifier:Modifier,selected:Int) {
    val day=selected+21
    val data=mealData[day] ?: mealData[21]!!
    var meal by rememberSaveable(day) { mutableIntStateOf(0) }
    LazyColumn(modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp,16.dp,16.dp,110.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        item {
            Text("Dove mangiare",fontSize=29.sp,fontWeight=FontWeight.Black,color=Navy)
            Text("Più scelta, ma regole nette: a pranzo street food incluso; la sera esclusivamente ristoranti.",fontSize=12.sp,color=Muted)
        }
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(meal==0,{meal=0},shape=SegmentedButtonDefaults.itemShape(0,2)){Text("Pranzo")}
                SegmentedButton(meal==1,{meal=1},shape=SegmentedButtonDefaults.itemShape(1,2)){Text("Cena")}
            }
        }
        item {
            val list=if(meal==0)data.first else data.second
            MealSection(if(meal==0) "PRANZO · STREET FOOD" else "SERA · SOLO RISTORANTI",list,if(meal==0) Sky else Sand)
        }
    }
}

@Composable
fun Weather(modifier:Modifier) {
    LazyColumn(modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp,16.dp,16.dp,110.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {
            Card(shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=Color.White)) {
                Column(Modifier.padding(20.dp)) {
                    Text("Meteo",fontSize=30.sp,fontWeight=FontWeight.Black,color=Navy)
                    Text("Il 23/09 è la giornata di mare. La scelta della costa resta subordinata alle condizioni reali.",fontSize=12.sp,color=Muted)
                    Spacer(Modifier.height(14.dp))
                    Text("Quando sei online, questa sezione è il punto di controllo prima di uscire.",fontWeight=FontWeight.Bold,fontSize=13.sp)
                }
            }
        }
        item { WeatherCard("23 SET","MARE","Controllare pioggia + vento + mare") }
        item { WeatherCard("24 SET","SAN CARLO","Arrivo a Napoli con margine") }
        item { WeatherCard("27 SET","BARRA","Controllare viabilità e affluenza") }
    }
}
@Composable
fun WeatherCard(date:String,title:String,note:String) {
    Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color.White)) {
        Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
            Icon(Icons.Default.WbSunny,null,tint=Color(0xFFE39A28),modifier=Modifier.size(30.dp))
            Spacer(Modifier.width(12.dp))
            Column { Text(date,fontSize=10.sp,fontWeight=FontWeight.Black,color=Blue); Text(title,fontSize=17.sp,fontWeight=FontWeight.ExtraBold); Text(note,fontSize=10.sp,color=Muted) }
        }
    }
}

@Composable
fun BottomBar(tab:Int,onTab:(Int)->Unit) {
    NavigationBar(containerColor=Color.White,windowInsets=WindowInsets.navigationBars) {
        listOf(Icons.Default.Home to "Viaggio",Icons.Default.Event to "Giorno",Icons.Default.WbSunny to "Meteo",Icons.Default.Restaurant to "Mangiare").forEachIndexed { i,(icon,label) ->
            NavigationBarItem(selected=tab==i,onClick={onTab(i)},icon={Icon(icon,label)},label={Text(label,fontSize=9.sp)})
        }
    }
}
