package com.example.sotacalc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val DarkSotaColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFBB86FC),
    secondary = androidx.compose.ui.graphics.Color(0xFF03DAC6),
    background = androidx.compose.ui.graphics.Color(0xFF121212),
    surface = androidx.compose.ui.graphics.Color(0xFF1E1E1E),
    onPrimary = androidx.compose.ui.graphics.Color.Black,
    onSecondary = androidx.compose.ui.graphics.Color.Black,
    onBackground = androidx.compose.ui.graphics.Color(0xFFE0E0E0),
    onSurface = androidx.compose.ui.graphics.Color(0xFFE0E0E0),
)

private val LargerTypography = Typography(
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 36.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 28.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 24.sp
    )
)

fun loadMagicSchoolsFromPython(): Map<String, List<String>> {
    val py = Python.getInstance()
    val pyModule = py.getModule("SotACalc")
    val pyDict = pyModule.get("MAGIC_SCHOOLS")?.asMap() ?: emptyMap()

    val schoolsMap = mutableMapOf<String, List<String>>()
    for ((key, value) in pyDict) {
        val schoolName = key.toString()
        val skillsList = value?.asList()?.map { it.toString() } ?: emptyList()
        schoolsMap[schoolName] = skillsList
    }
    return schoolsMap
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        setContent {
            MaterialTheme(
                colorScheme = DarkSotaColorScheme,
                typography = LargerTypography
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SotACalcApp()
                }
            }
        }
    }
}

sealed class Screen {
    object SchoolSelect : Screen()
    data class SkillSelect(val schoolName: String, val preSelectedSkills: List<String> = emptyList()) : Screen()
    data class OptimizationConfig(val schoolName: String, val selectedSkills: List<String>) : Screen()
    data class OptimizationResult(val schoolName: String, val results: Map<String, Int>, val totalExp: Long) : Screen()
}

@Composable
fun SotACalcApp() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.SchoolSelect) }

    when (val screen = currentScreen) {
        is Screen.SchoolSelect -> {
            SchoolSelectionScreen(
                onSchoolSelected = { schoolName ->
                    currentScreen = Screen.SkillSelect(schoolName, emptyList())
                }
            )
        }
        is Screen.SkillSelect -> {
            SkillSelectionScreen(
                schoolName = screen.schoolName,
                preSelectedSkills = screen.preSelectedSkills,
                onBack = { currentScreen = Screen.SchoolSelect },
                onContinue = { skills ->
                    currentScreen = Screen.OptimizationConfig(screen.schoolName, skills)
                }
            )
        }
        is Screen.OptimizationConfig -> {
            OptimizationConfigScreen(
                schoolName = screen.schoolName,
                selectedSkills = screen.selectedSkills,
                onBack = {
                    // Pass the currently selected skills back to SkillSelect
                    currentScreen = Screen.SkillSelect(screen.schoolName, screen.selectedSkills)
                },
                onOptimizationFinished = { results, totalExp ->
                    currentScreen = Screen.OptimizationResult(screen.schoolName, results, totalExp)
                }
            )
        }
        is Screen.OptimizationResult -> {
            OptimizationResultScreen(
                schoolName = screen.schoolName,
                results = screen.results,
                totalExp = screen.totalExp,
                onStartOver = { currentScreen = Screen.SchoolSelect }
            )
        }
    }
}

@Composable
fun SchoolSelectionScreen(onSchoolSelected: (String) -> Unit) {
    val magicSchools = loadMagicSchoolsFromPython()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome to SotACalc",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Added line
        Text(
            text = "By Mimner",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Which magic school do you want to optimize?",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        for (school in magicSchools.keys.sorted()) {
            Button(
                onClick = { onSchoolSelected(school) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(vertical = 4.dp)
            ) {
                Text(school, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun SkillSelectionScreen(
    schoolName: String,
    preSelectedSkills: List<String>,
    onBack: () -> Unit,
    onContinue: (List<String>) -> Unit
) {
    val magicSchools = loadMagicSchoolsFromPython()
    val availableSkills = magicSchools[schoolName] ?: emptyList()

    // Initialize using the preSelectedSkills passed from the screen state
    val selectedSkills = remember { mutableStateListOf<String>().apply { addAll(preSelectedSkills) } }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "$schoolName School",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Which ten skills would you like to use in the optimization?",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        for (skill in availableSkills) {
            val isSelected = selectedSkills.contains(skill)

            val buttonColors = if (isSelected) {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            }

            Button(
                onClick = {
                    if (isSelected) {
                        selectedSkills.remove(skill)
                    } else {
                        if (selectedSkills.size < 10) {
                            selectedSkills.add(skill)
                        }
                    }
                },
                colors = buttonColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(vertical = 4.dp)
            ) {
                Text(skill, style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onContinue(selectedSkills.toList()) },
            enabled = selectedSkills.size == 10,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Continue (${selectedSkills.size}/10)", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onBack) {
            Text("Back to Schools", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun OptimizationConfigScreen(
    schoolName: String,
    selectedSkills: List<String>,
    onBack: () -> Unit,
    onOptimizationFinished: (Map<String, Int>, Long) -> Unit
) {
    var targetAttunementInput by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    val lockedSkillsMap = remember { mutableStateMapOf<String, Int?>() }

    var activeSkillToLock by remember { mutableStateOf<String?>(null) }
    var tempLockValueInput by remember { mutableStateOf("") }

    var showImpossibleDialog by remember { mutableStateOf(false) }

    val targetAttunementInt = targetAttunementInput.toIntOrNull()
    val isTargetValid = targetAttunementInt != null && targetAttunementInt in 1..200

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Title and School Name split onto separate lines and centered
        Text(
            text = "Optimization Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = schoolName,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Target attunement prompt and (1-200) split onto separate lines and centered
        Text(
            text = "What is your target attunement?",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "(1-200)",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = targetAttunementInput,
            onValueChange = { targetAttunementInput = it },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Lock Skills (Optional)",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tap a skill to lock or edit its value:",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        for (skill in selectedSkills) {
            val lockedValue = lockedSkillsMap[skill]
            val isLocked = lockedValue != null

            val buttonColors = if (isLocked) {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            } else {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            }

            Button(
                onClick = {
                    tempLockValueInput = lockedValue?.toString() ?: ""
                    activeSkillToLock = skill
                },
                colors = buttonColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(vertical = 4.dp)
            ) {
                val buttonText = if (isLocked) "$skill (Locked: $lockedValue)" else skill
                Text(buttonText, style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val finalTarget = targetAttunementInt ?: 0

                val skillsToOptimizeList = selectedSkills.map { skill ->
                    val lockedVal = lockedSkillsMap[skill]
                    val isLocked = lockedVal != null
                    mapOf(
                        "skill" to skill,
                        "locked" to isLocked,
                        "locked_value" to lockedVal
                    )
                }

                try {
                    val py = Python.getInstance()
                    val pyModule = py.getModule("SotACalc")

                    val gameData = pyModule.callAttr("load_game_data")
                    val expBySkillLevel = gameData.asList()[0]
                    val skillModifiers = gameData.asList()[1]

                    val pyResult = pyModule.callAttr(
                        "optimize_skill_levels",
                        skillsToOptimizeList,
                        finalTarget,
                        expBySkillLevel,
                        skillModifiers
                    )

                    val resultArray = pyResult.asList()
                    val rawResultsMap = resultArray[0].asMap()
                    val totalExpLong = resultArray[1].toLong()

                    val parsedResults = mutableMapOf<String, Int>()
                    for ((k, v) in rawResultsMap) {
                        parsedResults[k.toString()] = v.toString().toInt()
                    }

                    onOptimizationFinished(parsedResults, totalExpLong)
                } catch (e: Exception) {
                    e.printStackTrace()
                    showImpossibleDialog = true
                }
            },
            enabled = isTargetValid,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Run Optimization", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onBack) {
            Text("Back to Skills", style = MaterialTheme.typography.labelLarge)
        }
    }

    if (activeSkillToLock != null) {
        val currentSkillName = activeSkillToLock!!
        var popupInput by remember { mutableStateOf(tempLockValueInput) }
        val popupInputInt = popupInput.toIntOrNull()
        val isPopupValid = popupInputInt != null && popupInputInt in 1..200

        AlertDialog(
            onDismissRequest = { activeSkillToLock = null },
            title = {
                Text(
                    text = "Lock Skill Value",
                    style = MaterialTheme.typography.headlineMedium
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter value (1-200) for '$currentSkillName':",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = popupInput,
                        onValueChange = { popupInput = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = {
                        lockedSkillsMap.remove(currentSkillName)
                        activeSkillToLock = null
                    }) {
                        Text(
                            text = "Remove Lock",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isPopupValid) {
                            lockedSkillsMap[currentSkillName] = popupInputInt
                            activeSkillToLock = null
                        }
                    },
                    enabled = isPopupValid
                ) {
                    Text("Confirm", style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(onClick = { activeSkillToLock = null }) {
                    Text("Cancel", style = MaterialTheme.typography.labelLarge)
                }
            }
        )
    }

    if (showImpossibleDialog) {
        AlertDialog(
            onDismissRequest = { showImpossibleDialog = false },
            title = {
                Text(
                    text = "Impossible Configuration",
                    style = MaterialTheme.typography.headlineMedium
                )
            },
            text = {
                Text(
                    text = "This target attunement cannot be reached with your current locked skills, as it would require unlocked skills to exceed level 200.",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                Button(
                    onClick = { showImpossibleDialog = false }
                ) {
                    Text("OK", style = MaterialTheme.typography.labelLarge)
                }
            }
        )
    }
}

@Composable
fun OptimizationResultScreen(
    schoolName: String,
    results: Map<String, Int>,
    totalExp: Long,
    onStartOver: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "$schoolName Results",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Total Experience Required: %,d".format(totalExp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        for ((skill, level) in results.entries.sortedByDescending { it.value }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = skill,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Lvl $level",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onStartOver,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Start Over", style = MaterialTheme.typography.labelLarge)
        }
    }
}