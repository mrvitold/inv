package com.example.invoicesaver

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Scaffold
import androidx.compose.material.TopAppBar
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Photo
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.invoicesaver.ui.theme.InvoicesaverTheme
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.edit
import coil.compose.rememberAsyncImagePainter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.navigationBarsPadding
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import java.util.UUID
import androidx.compose.foundation.layout.statusBarsPadding
import android.content.Intent

private const val PREFS_NAME = "invoice_prefs"
private const val KEY_IMAGE_LIST = "image_list"

data class InvoiceItem(val id: String, val uri: String, val uploadDate: String)

fun saveInvoiceItem(context: Context, uri: String) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val listJson = prefs.getString(KEY_IMAGE_LIST, "[]") ?: "[]"
    val list = JSONArray(listJson)
    val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
    val obj = JSONObject()
    obj.put("id", UUID.randomUUID().toString())
    obj.put("uri", uri)
    obj.put("uploadDate", date)
    list.put(obj)
    prefs.edit { putString(KEY_IMAGE_LIST, list.toString()) }
}

fun removeInvoiceItems(context: Context, ids: List<String>) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val listJson = prefs.getString(KEY_IMAGE_LIST, "[]") ?: "[]"
    val list = JSONArray(listJson)
    val newList = JSONArray()
    for (i in 0 until list.length()) {
        val obj = list.getJSONObject(i)
        if (!ids.contains(obj.getString("id"))) {
            newList.put(obj)
        }
    }
    prefs.edit { putString(KEY_IMAGE_LIST, newList.toString()) }
}

fun getInvoiceItems(context: Context): List<InvoiceItem> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val listJson = prefs.getString(KEY_IMAGE_LIST, "[]") ?: "[]"
    val list = JSONArray(listJson)
    val result = mutableListOf<InvoiceItem>()
    for (i in 0 until list.length()) {
        try {
            val entry = list.get(i)
            if (entry is JSONObject) {
                result.add(InvoiceItem(
                    entry.optString("id", System.currentTimeMillis().toString()),
                    entry.optString("uri", ""),
                    entry.optString("uploadDate", "(unknown)")
                ))
            } else if (entry is String) {
                // Old format: just a URI string
                result.add(InvoiceItem(
                    System.currentTimeMillis().toString(),
                    entry,
                    "(unknown)"
                ))
            } // else: skip invalid entry
        } catch (e: Exception) {
            // Skip invalid/corrupted entry
            continue
        }
    }
    return result
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            InvoicesaverTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = "main"
                ) {
                    composable("main") { MainScreen(navController) }
                    composable("gallery") { GalleryScreen(navController) }
                    composable("detail/{invoiceId}") { backStackEntry ->
                        val invoiceId = backStackEntry.arguments?.getString("invoiceId")
                        DetailScreen(invoiceId, navController)
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen(navController: NavHostController) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (e: SecurityException) {
                    // Ignore if already granted or not needed
                }
                saveInvoiceItem(context, uri.toString())
            }
            Toast.makeText(context, "${uris.size} invoice(s) uploaded!", Toast.LENGTH_SHORT).show()
        }
    }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Invoice Saver") }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Button(onClick = {
                launcher.launch("image/*")
            }) {
                Icon(Icons.Default.Add, contentDescription = "Upload")
                Text("Upload Invoice")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { navController.navigate("gallery") }) {
                Icon(Icons.Default.Photo, contentDescription = "Gallery")
                Text("Gallery")
            }
        }
    }
}

@Composable
fun GalleryScreen(navController: NavHostController) {
    val context = LocalContext.current
    var invoiceItems by remember { mutableStateOf(getInvoiceItems(context).filter { it.uri.isNotBlank() }) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    // Count occurrences of each URI
    val uriCounts = remember(invoiceItems) {
        invoiceItems.groupingBy { it.uri }.eachCount()
    }
    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text("Gallery") },
                actions = {
                    if (selectedIds.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Selected",
                            modifier = Modifier
                                .clickable { showDeleteDialog = true }
                                .padding(12.dp)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (invoiceItems.isEmpty()) {
            Text(
                text = "No invoices uploaded yet.",
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            Column(modifier = Modifier.padding(innerPadding)) {
                if (showDeleteDialog) {
                    androidx.compose.material.AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text("Delete selected invoices?") },
                        text = { Text("Are you sure you want to delete the selected invoices?") },
                        confirmButton = {
                            Button(onClick = {
                                removeInvoiceItems(context, selectedIds.toList())
                                invoiceItems = getInvoiceItems(context).filter { it.uri.isNotBlank() }
                                selectedIds = emptySet()
                                showDeleteDialog = false
                            }) { Text("Delete") }
                        },
                        dismissButton = {
                            Button(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                        }
                    )
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(invoiceItems, key = { it.id }) { item ->
                        androidx.compose.foundation.layout.Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 16.dp)
                        ) {
                            androidx.compose.material.Checkbox(
                                checked = selectedIds.contains(item.id),
                                onCheckedChange = { checked ->
                                    selectedIds = if (checked) selectedIds + item.id else selectedIds - item.id
                                },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            androidx.compose.foundation.layout.Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        navController.navigate("detail/${Uri.encode(item.id)}")
                                    }
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(item.uri),
                                    contentDescription = "Invoice thumbnail",
                                    modifier = Modifier
                                        .size(64.dp)
                                        .padding(end = 16.dp)
                                )
                                Column {
                                    Text(text = "Uploaded: ${item.uploadDate}")
                                    if ((uriCounts[item.uri] ?: 0) > 1) {
                                        Text(
                                            text = "duplicated",
                                            color = androidx.compose.ui.graphics.Color.Red,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun DetailScreen(invoiceId: String?, navController: NavHostController) {
    val context = LocalContext.current
    var invoiceItems by remember { mutableStateOf(getInvoiceItems(context).filter { it.uri.isNotBlank() }) }
    val startIndex = invoiceItems.indexOfFirst { it.id == invoiceId }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = startIndex)
    // Count occurrences of each URI
    val uriCounts = remember(invoiceItems) {
        invoiceItems.groupingBy { it.uri }.eachCount()
    }
    var showDeleteDialog by remember { mutableStateOf(false) }
    // Handle out-of-bounds after deletion
    if (invoiceItems.isEmpty()) {
        // If all items deleted, go back to gallery
        LaunchedEffect(Unit) { navController.popBackStack() }
        return
    }
    if (pagerState.currentPage >= invoiceItems.size) {
        // If current page is out of bounds, move to last page
        LaunchedEffect(invoiceItems.size) {
            pagerState.scrollToPage(invoiceItems.size - 1)
        }
    }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Invoice Detail") }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
        ) {
            HorizontalPager(
                count = invoiceItems.size,
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val invoice = invoiceItems[page]
                Image(
                    painter = rememberAsyncImagePainter(invoice.uri),
                    contentDescription = "Invoice image",
                    modifier = Modifier.fillMaxSize()
                )
            }
            val currentInvoice = invoiceItems.getOrNull(pagerState.currentPage)
            if (currentInvoice != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if ((uriCounts[currentInvoice.uri] ?: 0) > 1) {
                        Text(
                            text = "duplicated",
                            color = androidx.compose.ui.graphics.Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    Text(text = "Uploaded: ${currentInvoice.uploadDate}")
                    Button(onClick = { showDeleteDialog = true }, modifier = Modifier.padding(top = 8.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                        Text("Delete Invoice")
                    }
                }
            }
            if (showDeleteDialog && currentInvoice != null) {
                androidx.compose.material.AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Delete invoice?") },
                    text = { Text("Are you sure you want to delete this invoice?") },
                    confirmButton = {
                        Button(onClick = {
                            removeInvoiceItems(context, listOf(currentInvoice.id))
                            invoiceItems = getInvoiceItems(context).filter { it.uri.isNotBlank() }
                            showDeleteDialog = false
                        }) { Text("Delete") }
                    },
                    dismissButton = {
                        Button(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                    }
                )
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    InvoicesaverTheme {
        Greeting("Android")
    }
}