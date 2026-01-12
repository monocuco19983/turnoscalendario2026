package com.ejemplo.turnoscalendario2026

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

// ---------------------------
// MODELO
// ---------------------------

enum class TurnoGrupo { A, B, C, D, E }
enum class TipoTurno { M, T, N, D } // Mañana, Tarde, Noche, Descanso

// ---------------------------
// FECHA DE INICIO DEL CICLO
// ---------------------------
// Fecha de inicio común: 1 de enero de 2026 (según tu indicación)
private val FECHA_INICIO_CICLO: LocalDate = LocalDate.of(2026, 1, 1)

// ---------------------------
// PATRONES DE 35 DÍAS (según lo proporcionado)
// ---------------------------
// Cada string tiene 35 caracteres: D/M/T/N
// Convertimos cada carácter a TipoTurno y comprobamos longitud.

private fun parsePattern(s: String): List<TipoTurno> {
    val cleaned = s.trim().replace("\\s+".toRegex(), "")
    require(cleaned.length == 35) { "Cada patrón debe tener exactamente 35 caracteres" }
    return cleaned.map { ch ->
        when (ch.uppercaseChar()) {
            'M' -> TipoTurno.M
            'T' -> TipoTurno.T
            'N' -> TipoTurno.N
            'D' -> TipoTurno.D
            else -> TipoTurno.D
        }
    }
}

private val patronA = parsePattern("DDDDMMMTTTTDDDDDDDTTTNNNNDDDMMMMNNN")
private val patronB = parsePattern("NNNNDDDMMMMNNNDDDDMMMTTTTDDDDDDDTTT")
private val patronC = parsePattern("TTTTDDDDDDDTTTNNNNDDDMMMMNNNDDDDMMM")
private val patronD = parsePattern("DDDDTTTNNNNDDDMMMMNNNDDDDMMMTTTTDDD")
private val patronE = parsePattern("MMMMNNNDDDDMMMTTTTDDDDDDDTTTNNNNDDD")

private val patrones35: Map<TurnoGrupo, List<TipoTurno>> = mapOf(
    TurnoGrupo.A to patronA,
    TurnoGrupo.B to patronB,
    TurnoGrupo.C to patronC,
    TurnoGrupo.D to patronD,
    TurnoGrupo.E to patronE
)

// ---------------------------
// OBTENER TURNO PARA UNA FECHA (CICLO 35)
// ---------------------------

fun getTurnoParaFechaCiclo35(grupo: TurnoGrupo, fecha: LocalDate): TipoTurno {
    val patron = patrones35[grupo] ?: return TipoTurno.D
    if (patron.size != 35) return TipoTurno.D
    val dias = ChronoUnit.DAYS.between(FECHA_INICIO_CICLO, fecha).toInt()
    val indice = ((dias % 35) + 35) % 35
    return patron[indice]
}

// Colores para cada tipo de turno
fun colorParaTurno(turno: TipoTurno): Color {
    return when (turno) {
        TipoTurno.M -> Color(0xFFFFFF99) // Amarillo suave
        TipoTurno.T -> Color(0xFFB2FFB2) // Verde suave
        TipoTurno.N -> Color(0xFFB2CFFF) // Azul suave
        TipoTurno.D -> Color(0xFFE0E0E0) // Gris claro (descanso)
    }
}

// ---------------------------
// FESTIVOS NACIONALES ESPAÑA (BÁSICO)
// ---------------------------

fun festivosNacionales(year: Int): Set<LocalDate> {
    return setOf(
        LocalDate.of(year, 1, 1),   // Año Nuevo
        LocalDate.of(year, 1, 6),   // Reyes
        LocalDate.of(year, 5, 1),   // Día del Trabajador
        LocalDate.of(year, 8, 15),  // Asunción
        LocalDate.of(year, 10, 12), // Fiesta Nacional
        LocalDate.of(year, 11, 1),  // Todos los Santos
        LocalDate.of(year, 12, 6),  // Constitución
        LocalDate.of(year, 12, 8),  // Inmaculada
        LocalDate.of(year, 12, 25)  // Navidad
    )
}

// ---------------------------
// PREFERENCIAS (GUARDAR TURNO Y AÑO)
// ---------------------------

private const val PREFS_NAME = "turnos_prefs"
private const val KEY_TURNO = "turno"
private const val KEY_ANO = "ano"

fun guardarPreferencias(context: Context, grupo: TurnoGrupo, ano: Int) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit()
        .putString(KEY_TURNO, grupo.name)
        .putInt(KEY_ANO, ano)
        .apply()
}

fun cargarPreferencias(context: Context): Pair<TurnoGrupo, Int> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val turnoString = prefs.getString(KEY_TURNO, TurnoGrupo.A.name) ?: TurnoGrupo.A.name
    val ano = prefs.getInt(KEY_ANO, LocalDate.now().year)
    val turno = TurnoGrupo.valueOf(turnoString)
    return turno to ano
}

// ---------------------------
// ACTIVITY PRINCIPAL
// ---------------------------

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val (turnoGuardado, anoGuardado) = cargarPreferencias(this)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PantallaPrincipal(
                        turnoInicial = turnoGuardado,
                        anoInicial = anoGuardado,
                        onGuardarPreferencias = { grupo, ano ->
                            guardarPreferencias(this, grupo, ano)
                        }
                    )
                }
            }
        }
    }
}

// ---------------------------
// UI
// ---------------------------

@Composable
fun PantallaPrincipal(
    turnoInicial: TurnoGrupo,
    anoInicial: Int,
    onGuardarPreferencias: (TurnoGrupo, Int) -> Unit
) {
    var grupoSeleccionado by remember { mutableStateOf(turnoInicial) }
    var anoSeleccionado by remember { mutableStateOf(anoInicial) }

    val festivos = remember(anoSeleccionado) { festivosNacionales(anoSeleccionado) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Turno:",
                modifier = Modifier.padding(end = 8.dp),
                fontWeight = FontWeight.Bold
            )

            DropdownTurnos(
                grupoSeleccionado = grupoSeleccionado,
                onGrupoChange = {
                    grupoSeleccionado = it
                    onGuardarPreferencias(grupoSeleccionado, anoSeleccionado)
                }
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = "Año:",
                modifier = Modifier.padding(end = 8.dp),
                fontWeight = FontWeight.Bold
            )

            DropdownAnos(
                anoSeleccionado = anoSeleccionado,
                onAnoChange = {
                    anoSeleccionado = it
                    onGuardarPreferencias(grupoSeleccionado, anoSeleccionado)
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Calendario anual $anoSeleccionado - Turno $grupoSeleccionado",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn {
            items((1..12).toList()) { mes ->
                CalendarioMes(
                    year = anoSeleccionado,
                    month = mes,
                    grupo = grupoSeleccionado,
                    festivos = festivos
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun DropdownTurnos(
    grupoSeleccionado: TurnoGrupo,
    onGrupoChange: (TurnoGrupo) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Button(onClick = { expanded = true }) {
            Text(text = grupoSeleccionado.name)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            TurnoGrupo.values().forEach { grupo ->
                DropdownMenuItem(
                    text = { Text(text = grupo.name) },
                    onClick = {
                        onGrupoChange(grupo)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun DropdownAnos(
    anoSeleccionado: Int,
    onAnoChange: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val anoActual = LocalDate.now().year
    val anos = (anoActual - 5..anoActual + 5).toList()

    Box {
        Button(onClick = { expanded = true }) {
            Text(text = anoSeleccionado.toString())
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            anos.forEach { ano ->
                DropdownMenuItem(
                    text = { Text(text = ano.toString()) },
                    onClick = {
                        onAnoChange(ano)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun CalendarioMes(
    year: Int,
    month: Int,
    grupo: TurnoGrupo,
    festivos: Set<LocalDate>
) {
    val yearMonth = YearMonth.of(year, month)
    val primerDia = yearMonth.atDay(1)
    val diasEnMes = yearMonth.lengthOfMonth()

    val nombreMes = when (month) {
        1 -> "ENERO"
        2 -> "FEBRERO"
        3 -> "MARZO"
        4 -> "ABRIL"
        5 -> "MAYO"
        6 -> "JUNIO"
        7 -> "JULIO"
        8 -> "AGOSTO"
        9 -> "SEPTIEMBRE"
        10 -> "OCTUBRE"
        11 -> "NOVIEMBRE"
        12 -> "DICIEMBRE"
        else -> ""
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
    ) {
        Text(
            text = nombreMes,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            textAlign = TextAlign.Center
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            val diasSemana = listOf("L", "M", "X", "J", "V", "S", "D")
            diasSemana.forEach { dia ->
                Text(
                    text = dia,
                    modifier = Modifier
                        .weight(1f)
                        .padding(2.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        var diaActual = 1
        var diaSemanaInicio = primerDia.dayOfWeek.value // 1=Lunes

        while (diaActual <= diasEnMes) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 1..7) {
                    if ((diaActual == 1 && col < diaSemanaInicio) || diaActual > diasEnMes) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                        ) {}
                    } else {
                        val fecha = LocalDate.of(year, month, diaActual)
                        val turno = getTurnoParaFechaCiclo35(grupo, fecha)

                        val baseColor = colorParaTurno(turno)
                        val esFestivo = festivos.contains(fecha)
                        val colorFinal = if (esFestivo) Color(0xFFFFB3B3) else baseColor

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .padding(1.dp)
                                .background(colorFinal),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = diaActual.toString(),
                                fontSize = 11.sp,
                                fontWeight = if (esFestivo) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = turno.name,
                                fontSize = 10.sp
                            )
                        }
                        diaActual++
                    }
                }
            }
        }
    }
}
