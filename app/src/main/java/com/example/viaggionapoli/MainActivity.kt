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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val Navy = Color(0xFF0B2237)
private val Blue = Color(0xFF247DB8)
private val Sky = Color(0xFFEAF4FA)
private val Sand = Color(0xFFF4E8D5)
private val Green = Color(0xFFE5F5F1)
private val Rose = Color(0xFFF8E6E7)
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
      Place("L'Antica Pizzeria Da Michele Pompei","Pompei · pranzo veloce","street food"),
      Place("Na' Pasta","Pompei · pasta napoletana","street food"),
      Place("Le Delizie Pompei","Pompei · pizza e fritti","street food"),
      Place("Varnelli Pizza Bistrot & Restaurant","Pompei · pizza","street food"),
      Place("Mercato Pompeiano","Pompei · pizza","street food"),
      Place("Add'ù Mimi","Pompei · cucina napoletana veloce","street food")
    ),
    listOf(
      Place("Spinelli Bistrot","Piazza Bartolo Longo · ristorante","ristorante"),
      Place("Varnelli Pizza Bistrot & Restaurant","Pompei · ristorante/pizzeria","ristorante"),
      Place("Caupona","Pompei · cucina mediterranea","ristorante"),
      Place("Bistrot Fratelli Cannavacciuolo","Pompei · cucina italiana e pesce","ristorante"),
      Place("Stuzzicò by Lucius","Pompei · cucina italiana e pesce","ristorante"),
      Place("Ristorante Il Principe","Pompei · cucina italiana e pesce","ristorante")
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
      Place("La Locanda del Monacone","Rione Sanità · cucina napoletana","ristorante"),
      Place("Vicus - Trattoria Napoletana","Rione Sanità · cucina napoletana","ristorante"),
      Place("Concettina ai Tre Santi","Sanità · pizzeria con servizio","ristorante"),
      Place("Starita","Materdei · pizza e cucina","ristorante"),
      Place("La Locanda del Monacone","Sanità · ristorante","ristorante"),
      Place("Vicus - Trattoria Napoletana","Sanità · cucina napoletana","ristorante")
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
      Place("Ermenegildo","Barra · pizzeria con servizio","ristorante"),
      Place("Pub & Grill Cozzolino","Barra · ristorante","ristorante"),
      Place("Pizzeria Mario dal 1970","Barra · pizzeria","ristorante"),
      Place("Pizzeria Bisignano","Barra · pizzeria","ristorante"),
      Place("Risto Pub Addu' Sabina","Barra · cucina informale","ristorante"),
      Place("Trattoria Prisco","Barra · cucina campana","ristorante")
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

data class Photo(val url:String,val caption:String)
data class WeatherLocation(val name:String,val lat:Double,val lon:Double)
data class WeatherHour(val time:String,val temperature:Int,val rainProbability:Int,val wind:Int,val code:Int)
data class WeatherDay(val code:Int,val max:Double,val min:Double,val rainProbability:Int,val windMax:Int,val hours:List<WeatherHour>)

val dayLocations = mapOf(
  21 to WeatherLocation("Napoli · Chiaia",40.8335,14.2240),
  22 to WeatherLocation("Napoli · centro",40.8518,14.2681),
  23 to WeatherLocation("Costa · riferimento Sorrento",40.6263,14.3754),
  24 to WeatherLocation("Napoli · San Carlo",40.8394,14.2508),
  25 to WeatherLocation("Pompei",40.7485,14.4848),
  26 to WeatherLocation("Napoli · Sanità / Materdei",40.8678,14.2465),
  27 to WeatherLocation("Napoli · Barra",40.8454,14.3094),
  28 to WeatherLocation("Pompei · partenza",40.7485,14.4848)
)

val dayPhotos = mapOf(
  21 to listOf(
    Photo("https://a.travel-assets.com/findyours-php/viewfinder/images/res70/350000/350403-Via-Caracciolo-E-Lungomare-Di-Napoli.jpg","Lungomare di Napoli"),
    Photo("https://images.partir.com/YqMH_FoyTAnkuOlJLo1HljcMJfs%3D/800x/ou-se-loger/italie-naples-hotel-spaccanapoli.jpg","Centro storico")
  ),
  22 to listOf(
    Photo("https://ak-d.tripcdn.com/images/1mi2u224x9417qdv6BB1D.jpg?proc=source%2Ftrip","Via dei Tribunali"),
    Photo("https://cosedinapoli.com/wp-content/uploads/2023/04/Palazzo-dAngio-5-scaled.jpeg","Strade del centro")
  ),
  23 to listOf(
    Photo("https://cdn.blastness.biz/media/1254/top/thumbs/full/marina-grande-sorrento01-1920.jpg","Marina Grande · Sorrento"),
    Photo("https://cdn2.civitatis.com/italia/napoles/galeria/positano-costa-amalfitana-napoles.jpg","Positano · Costiera")
  ),
  24 to listOf(
    Photo("https://www.napolidavivere.it/wp-content/uploads/2025/02/WhatsApp-Image-2025-02-04-at-12.43.33.jpeg","Teatro San Carlo"),
    Photo("https://www.codalario.com/datos/0/san_carlo357.jpg","Sala del San Carlo")
  ),
  25 to listOf(
    Photo("https://thetravellingsquid.com/wp-content/uploads/2020/05/forum-pompeii.jpg","Foro di Pompei"),
    Photo("https://www.carwiz.it/data/public/napoli/rsz_pompeii.jpg","Pompei e Vesuvio"),
    Photo("https://bluekeys.it/assets/images/blog/pompeii-tour-from-sorrento.jpg","Strada colonnata")
  ),
  26 to listOf(
    Photo("https://www.leisure-italy.com/wp-content/uploads/2018/11/Naples-Underground-Catacombe-di-S.Gennaro-1.jpg","Napoli sotterranea"),
    Photo("https://commons.wikimedia.org/wiki/Special:FilePath/FontanelleNaples3.JPG?width=1200","Cimitero delle Fontanelle")
  ),
  27 to listOf(
    Photo("https://www.photo4u.it/rep_upl/1506514985_71479_741997/o_1br1klgldra7r971p5h68hhr9k.jpg","Festa dei Gigli · Barra"),
    Photo("https://cosedinapoli.com/wp-content/uploads/2023/04/Palazzo-dAngio-5-scaled.jpeg","Napoli")
  ),
  28 to listOf(
    Photo("https://a.travel-assets.com/findyours-php/viewfinder/images/res70/350000/350403-Via-Caracciolo-E-Lungomare-Di-Napoli.jpg","Ultimo sguardo al Golfo"),
    Photo("https://thetravellingsquid.com/wp-content/uploads/2020/05/forum-pompeii.jpg","Pompei")
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
            primary=Navy, secondary=Blue, background=Color(0xFFF4F7FA),
            surface=Color.White, onSurface=Ink
        ),
        typography=Typography(
            headlineLarge=androidx.compose.ui.text.TextStyle(fontSize=30.sp,fontWeight=FontWeight.Black),
            titleLarge=androidx.compose.ui.text.TextStyle(fontSize=20.sp,fontWeight=FontWeight.ExtraBold)
        )
    ) {
        var selected by rememberSaveable { mutableIntStateOf(0) }
        var tab by rememberSaveable { mutableIntStateOf(0) }
        Scaffold(
            containerColor=Color(0xFFF4F7FA),
            topBar={AppTopBar()},
            bottomBar={BottomBar(tab){tab=it}}
        ) { pad ->
            when(tab) {
                0 -> Home(Modifier.padding(pad),selected){ selected=it; tab=1 }
                1 -> DayDetail(Modifier.padding(pad),selected)
                else -> Dining(Modifier.padding(pad),selected)
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
    LazyColumn(modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp,16.dp,16.dp,110.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item {
            Card(shape=RoundedCornerShape(28.dp),colors=CardDefaults.cardColors(containerColor=Color.White)) {
                Column(Modifier.padding(20.dp)) {
                    Text("Il viaggio, in riquadri.",fontSize=29.sp,fontWeight=FontWeight.Black,color=Navy)
                    Spacer(Modifier.height(6.dp))
                    Text("Foto, meteo live e proposte in zona. L'itinerario resta quello semi-definitivo.",color=Muted,fontSize=13.sp)
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth()) {
                        StatPill("1","giornata mare")
                        StatPill("19:00","San Carlo")
                        StatPill("18:00+","Gigli")
                    }
                }
            }
        }
        item { Text("ITINERARIO",fontSize=11.sp,fontWeight=FontWeight.Black,color=Muted,letterSpacing=1.3.sp) }
        items(days.indices.toList()) { i -> DayCard(days[i],i==selected){onSelect(i)} }
    }
}

@Composable
fun RowScope.StatPill(value:String,label:String) {
    Surface(shape=RoundedCornerShape(15.dp),color=Sky,modifier=Modifier.weight(1f)) {
        Column(Modifier.padding(10.dp)) {
            Text(value,fontWeight=FontWeight.Black,fontSize=16.sp,color=Navy)
            Text(label,fontSize=9.sp,color=Muted,maxLines=1)
        }
    }
}

@Composable
fun DayCard(day:Day,active:Boolean,onClick:()->Unit) {
    val dayNumber=21+days.indexOf(day)
    val photos=dayPhotos[dayNumber].orEmpty()
    Card(onClick=onClick,shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=Color.White),
        border=if(active) androidx.compose.foundation.BorderStroke(2.dp,day.accent) else null) {
        Column {
            if(photos.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().height(120.dp)) {
                    AsyncImage(model=photos[0].url,contentDescription=photos[0].caption,contentScale=ContentScale.Crop,modifier=Modifier.weight(1.35f).fillMaxHeight())
                    if(photos.size>1) {
                        AsyncImage(model=photos[1].url,contentDescription=photos[1].caption,contentScale=ContentScale.Crop,modifier=Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
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
}

@Composable
fun DayDetail(modifier:Modifier,selected:Int) {
    val d=days[selected]
    val dayNumber=selected+21
    val events=when(selected) {
        0 -> listOf("11:00 · Partenza da Cervia","13:00–14:00 · Pausa ricarica","18:00–20:00 · Chiaia","20:00–21:15 · Chiaia → Pompei","21:15+ · Arrivo + cena")
        1 -> listOf("Mattina · Napoli centro","11:30 · Giacca + camicia","Pomeriggio · Spaccanapoli / Tribunali","Sera · cena in ristorante")
        2 -> listOf("Mattina · partenza per il mare","Giornata · mare e relax","Sera · rientro")
        3 -> listOf("Mattina · caffè dai parenti","16:00 · trasferimento verso San Carlo","19:00 · Mitridate, re di Ponto","Dopo · rientro")
        4 -> listOf("Mattina · Pompei Scavi","Pranzo · pausa","Pomeriggio · seconda parte / rientro")
        5 -> listOf("Mattina · Napoli centro","Slot prenotato · Napoli Sotterranea da Gambrinus","Pomeriggio · Sanità","Pomeriggio/sera · Cimitero delle Fontanelle")
        6 -> listOf("Pomeriggio · arrivo verso Barra","17:00 · orientamento in zona","18:00+ · seconda parte della Ballata","Sera · rientro")
        else -> listOf("Mattina · check-out","Giornata · rientro")
    }
    val meals=mealData[dayNumber]
    LazyColumn(modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp,16.dp,16.dp,110.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { HeaderDay(d) }
        item { DayGallery(dayNumber) }
        item { WeatherInline(dayNumber) }
        item {
            if(selected==6) {
                Card(shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=Sand)) {
                    Column(Modifier.padding(15.dp)) {
                        Text("GIGLI · SERATA FINALE",fontSize=10.sp,fontWeight=FontWeight.Black,color=Navy,letterSpacing=1.2.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("Seconda parte della Ballata dalle 18:00+",fontSize=15.sp,fontWeight=FontWeight.ExtraBold)
                        Text("La giornata resta dedicata alla parte finale dell'evento, senza aggiungere tappe.",fontSize=11.sp,color=Muted)
                    }
                }
            }
        }
        items(events){EventRow(it)}
        if(meals!=null) {
            item { MealSection("PRANZO · PROPOSTE IN ZONA",meals.first,Sky) }
            item { MealSection("SERA · PROPOSTE IN ZONA",meals.second,Sand) }
        }
    }
}

@Composable
fun HeaderDay(d:Day) {
    Card(shape=RoundedCornerShape(26.dp),colors=CardDefaults.cardColors(containerColor=Color.White)) {
        Column(Modifier.padding(20.dp)) {
            Text(d.date,color=Blue,fontSize=10.sp,fontWeight=FontWeight.Black,letterSpacing=1.3.sp)
            Text(d.title,fontSize=28.sp,fontWeight=FontWeight.Black,color=Navy)
            Text(d.subtitle,color=Muted,fontSize=12.sp)
        }
    }
}

@Composable
fun DayGallery(day:Int) {
    val photos=dayPhotos[day].orEmpty()
    if(photos.isEmpty()) return
    LazyRow(horizontalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(end=4.dp)) {
        items(photos) { p ->
            Box(Modifier.width(250.dp).height(155.dp).clip(RoundedCornerShape(20.dp))) {
                AsyncImage(model=p.url,contentDescription=p.caption,contentScale=ContentScale.Crop,modifier=Modifier.fillMaxSize())
                Surface(color=Color.Black.copy(alpha=.45f),modifier=Modifier.align(Alignment.BottomStart)) {
                    Text(p.caption,color=Color.White,fontSize=10.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=9.dp,vertical=7.dp))
                }
            }
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
    Card(shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=Color.White)) {
        Column(Modifier.padding(14.dp)) {
            Surface(shape=RoundedCornerShape(11.dp),color=bg) {
                Text(title,Modifier.padding(8.dp),fontSize=10.sp,fontWeight=FontWeight.Black,color=Navy)
            }
            Spacer(Modifier.height(8.dp))
            places.forEachIndexed { i,p ->
                PlaceRow(p,i+1)
                if(i<places.lastIndex) HorizontalDivider(color=Color(0xFFE8EDF1),modifier=Modifier.padding(vertical=5.dp))
            }
        }
    }
}

fun siteFor(name:String):String? = when(name) {
    "L'Antica Pizzeria Di Matteo" -> "https://www.pizzeriadimatteo.com/"
    "L'Antica Pizzeria Da Michele","L'Antica Pizzeria Da Michele Pompei" -> "https://www.damichele.net/"
    "Gino e Toto Sorbillo" -> "https://www.sorbillo.it/"
    "Pasticceria Poppella","Poppella" -> "https://www.poppella.it/"
    "La Masardona" -> "https://www.lamasardona.it/"
    "Concettina ai Tre Santi" -> "https://www.concettinaitresanti.it/"
    "Ristorante Umberto" -> "https://www.ristoranteumberto.it/"
    "Starita","Pizzeria Starita" -> "https://www.pizzeriastarita.it/"
    "Caupona" -> "https://www.caupona.com/"
    "Cimitero delle Fontanelle" -> "https://cimiterodellefontanelle.it/"
    else -> null
}

@Composable
fun PlaceRow(p:Place,n:Int) {
    val context=LocalContext.current
    val site=siteFor(p.name)
    Row(Modifier.fillMaxWidth().padding(vertical=5.dp),verticalAlignment=Alignment.CenterVertically) {
        Text(String.format("%02d",n),fontSize=10.sp,fontWeight=FontWeight.Black,color=Muted,modifier=Modifier.width(28.dp))
        Column(Modifier.weight(1f)) {
            Text(p.name,fontSize=12.sp,fontWeight=FontWeight.Bold)
            Text(p.detail,fontSize=10.sp,color=Muted,maxLines=2,overflow=TextOverflow.Ellipsis)
            Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                if(site!=null) {
                    TextButton(onClick={context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(site)))},contentPadding=PaddingValues(0.dp)) {
                        Icon(Icons.Default.Language,null,modifier=Modifier.size(15.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("Sito",fontSize=10.sp)
                    }
                }
                TextButton(onClick={
                    val q=Uri.encode(p.name+" "+p.detail)
                    context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/maps/search/?api=1&query="+q)))
                },contentPadding=PaddingValues(0.dp)) {
                    Icon(Icons.Default.Place,null,modifier=Modifier.size(15.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("Maps",fontSize=10.sp)
                }
            }
        }
    }
}

suspend fun loadWeather(location:WeatherLocation,date:String):WeatherDay? = withContext(Dispatchers.IO) {
    try {
        val url="https://api.open-meteo.com/v1/forecast?latitude="+location.lat+"&longitude="+location.lon+"&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,windspeed_10m_max&hourly=temperature_2m,precipitation_probability,windspeed_10m,weather_code&timezone=Europe%2FRome&start_date="+date+"&end_date="+date
        val connection=java.net.URL(url).openConnection() as java.net.HttpURLConnection
        connection.connectTimeout=7000
        connection.readTimeout=7000
        connection.requestMethod="GET"
        val body=connection.inputStream.bufferedReader().use{it.readText()}
        connection.disconnect()

        val root=org.json.JSONObject(body)
        val daily=root.getJSONObject("daily")
        val max=daily.getJSONArray("temperature_2m_max").getDouble(0)
        val min=daily.getJSONArray("temperature_2m_min").getDouble(0)
        val rain=daily.getJSONArray("precipitation_probability_max").getInt(0)
        val wind=daily.getJSONArray("windspeed_10m_max").getInt(0)
        val code=daily.getJSONArray("weather_code").getInt(0)

        val hourly=root.getJSONObject("hourly")
        val temps=hourly.getJSONArray("temperature_2m")
        val probs=hourly.getJSONArray("precipitation_probability")
        val winds=hourly.getJSONArray("windspeed_10m")
        val codes=hourly.getJSONArray("weather_code")
        val times=hourly.getJSONArray("time")
        val hours=mutableListOf<WeatherHour>()
        listOf(8,10,12,14,16,18,20).forEach { idx ->
            if(idx<temps.length()) {
                hours += WeatherHour(
                    times.getString(idx).substringAfter("T").take(5),
                    temps.getDouble(idx).toInt(),
                    probs.getInt(idx),
                    winds.getDouble(idx).toInt(),
                    codes.getInt(idx)
                )
            }
        }
        WeatherDay(code,max,min,rain,wind,hours)
    } catch(_:Exception) {
        null
    }
}

fun weatherLabel(code:Int):String = when(code) {
    0 -> "Sereno"
    1,2 -> "Poco nuvoloso"
    3 -> "Coperto"
    45,48 -> "Nebbia"
    51,53,55,56,57 -> "Pioviggine"
    61,63,65,66,67 -> "Pioggia"
    71,73,75,77 -> "Neve"
    80,81,82 -> "Rovesci"
    95,96,99 -> "Temporale"
    else -> "Variabile"
}

fun weatherEmoji(code:Int):String = when(code) {
    0 -> "☀️"
    1,2 -> "🌤️"
    3 -> "☁️"
    45,48 -> "🌫️"
    51,53,55,56,57 -> "🌦️"
    61,63,65,66,67 -> "🌧️"
    71,73,75,77 -> "❄️"
    80,81,82 -> "🌦️"
    95,96,99 -> "⛈️"
    else -> "🌤️"
}

@Composable
fun WeatherInline(day:Int) {
    val context=LocalContext.current
    val location=dayLocations[day] ?: dayLocations[21]!!
    val date=String.format("2026-09-%02d",day)
    val state=produceState<WeatherDay?>(initialValue=null,location.name,day) {
        value=loadWeather(location,date)
    }
    Card(shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=Color.White)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("METEO · "+location.name.uppercase(),fontSize=10.sp,fontWeight=FontWeight.Black,color=Blue,letterSpacing=1.1.sp)
                    Text(
                        if(day==23) "Riferimento costiero; la località finale resta quella scelta in base al meteo."
                        else "Aggiornato quando apri l'app",
                        fontSize=10.sp,color=Muted
                    )
                }
                Text(weatherEmoji(state.value?.code ?: 0),fontSize=28.sp)
            }
            Spacer(Modifier.height(12.dp))
            if(state.value==null) {
                Text("Caricamento previsioni…",fontSize=13.sp,color=Muted)
            } else {
                val w=state.value!!
                Row(horizontalArrangement=Arrangement.spacedBy(7.dp),modifier=Modifier.fillMaxWidth()) {
                    Box(Modifier.weight(1f)){WeatherMetric(w.min.toInt().toString()+"°","min")}
                    Box(Modifier.weight(1f)){WeatherMetric(w.max.toInt().toString()+"°","max")}
                    Box(Modifier.weight(1f)){WeatherMetric(w.rainProbability.toString()+"%","pioggia")}
                    Box(Modifier.weight(1f)){WeatherMetric(w.windMax.toString()+" km/h","vento max")}
                }
                Spacer(Modifier.height(10.dp))
                Text(weatherLabel(w.code),fontSize=15.sp,fontWeight=FontWeight.ExtraBold,color=Navy)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                    items(w.hours) { h ->
                        Surface(shape=RoundedCornerShape(13.dp),color=Sky) {
                            Column(Modifier.padding(horizontal=10.dp,vertical=8.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                                Text(h.time,fontSize=10.sp,fontWeight=FontWeight.Black,color=Navy)
                                Text(weatherEmoji(h.code),fontSize=17.sp)
                                Text(h.temperature.toString()+"°",fontSize=11.sp,fontWeight=FontWeight.Bold)
                                Text(h.rainProbability.toString()+"% · "+h.wind.toString()+" km/h",fontSize=8.sp,color=Muted)
                            }
                        }
                    }
                }
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick={context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://open-meteo.com/")))}){Text("Fonte meteo")}
                    TextButton(onClick={context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.meteoam.it/")))}){Text("MeteoAM")}
                }
            }
        }
    }
}

@Composable
fun WeatherMetric(value:String,label:String) {
    Surface(shape=RoundedCornerShape(13.dp),color=Color(0xFFF4F7FA),modifier=Modifier.fillMaxWidth()) {
        Column(Modifier.padding(9.dp)) {
            Text(value,fontWeight=FontWeight.Black,fontSize=14.sp,color=Navy)
            Text(label,fontSize=8.sp,color=Muted)
        }
    }
}

@Composable
fun Dining(modifier:Modifier,selected:Int) {
    val day=selected+21
    val data=mealData[day] ?: mealData[21]!!
    var meal by rememberSaveable(day){mutableIntStateOf(0)}
    LazyColumn(modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp,16.dp,16.dp,110.dp),verticalArrangement=Arrangement.spacedBy(11.dp)) {
        item {
            Text("Dove mangiare",fontSize=30.sp,fontWeight=FontWeight.Black,color=Navy)
            Text("Proposte legate alla zona della giornata. Nessuna prenotazione incorporata: apri sito o Maps e scegli sul momento.",fontSize=12.sp,color=Muted)
        }
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(meal==0,{meal=0},shape=SegmentedButtonDefaults.itemShape(0,2)){Text("Pranzo")}
                SegmentedButton(meal==1,{meal=1},shape=SegmentedButtonDefaults.itemShape(1,2)){Text("Sera")}
            }
        }
        item {
            val list=if(meal==0)data.first else data.second
            MealSection(if(meal==0) "PRANZO · STREET FOOD / VELOCE" else "SERA · RISTORANTI",list,if(meal==0) Sky else Sand)
        }
    }
}

@Composable
fun BottomBar(tab:Int,onTab:(Int)->Unit) {
    NavigationBar(containerColor=Color.White,windowInsets=WindowInsets.navigationBars) {
        listOf(
            Icons.Default.Home to "Viaggio",
            Icons.Default.Event to "Giorno",
            Icons.Default.Restaurant to "Mangiare"
        ).forEachIndexed { i,(icon,label) ->
            NavigationBarItem(selected=tab==i,onClick={onTab(i)},icon={Icon(icon,label)},label={Text(label,fontSize=9.sp)})
        }
    }
}
