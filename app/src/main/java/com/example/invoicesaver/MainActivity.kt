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

private const val PREFS_NAME = "invoice_prefs"
private const val KEY_IMAGE_LIST = "image_list"

data class InvoiceItem(val uri: String, val uploadDate: String)

fun saveInvoiceItem(context: Context, uri: String) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val listJson = prefs.getString(KEY_IMAGE_LIST, "[]") ?: "[]"
    val list = JSONArray(listJson)
    val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
    val obj = JSONObject()
    obj.put("uri", uri)
    obj.put("uploadDate", date)
    list.put(obj)
    prefs.edit { putString(KEY_IMAGE_LIST, list.toString()) }
}

fun removeInvoiceItems(context: Context, uris: List<String>) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val listJson = prefs.getString(KEY_IMAGE_LIST, "[]") ?: "[]"
    val list = JSONArray(listJson)
    val newList = JSONArray()
    for (i in 0 until list.length()) {
        val obj = list.getJSONObject(i)
        if (!uris.contains(obj.getString("uri"))) {
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
        val obj = list.getJSONObject(i)
        result.add(InvoiceItem(obj.getString("uri"), obj.getString("uploadDate")))
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
                    composable("detail/{imageUri}") { backStackEntry ->
                        val imageUri = backStackEntry.arguments?.getString("imageUri")
                        DetailScreen(imageUri, navController)
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
    var invoiceItems by remember { mutableStateOf(getInvoiceItems(context)) }
    var selectedUris by remember { mutableStateOf(setOf<String>()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gallery") },
                actions = {
                    if (selectedUris.isNotEmpty()) {
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
                                removeInvoiceItems(context, selectedUris.toList())
                                invoiceItems = getInvoiceItems(context)
                                selectedUris = emptySet()
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
                    items(invoiceItems) { item ->
                        androidx.compose.foundation.layout.Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    if (selectedUris.isNotEmpty()) {
                                        selectedUris = if (selectedUris.contains(item.uri))
                                            selectedUris - item.uri else selectedUris + item.uri
                                    } else {
                                        navController.navigate("detail/${Uri.encode(item.uri)}")
                                    }
                                }
                                .padding(8.dp)
                        ) {
                            androidx.compose.material.Checkbox(
                                checked = selectedUris.contains(item.uri),
                                onCheckedChange = { checked ->
                                    selectedUris = if (checked) selectedUris + item.uri else selectedUris - item.uri
                                }
                            )
                            Image(
                                painter = rememberAsyncImagePainter(item.uri),
                                contentDescription = "Invoice thumbnail",
                                modifier = Modifier.height(64.dp)
                            )
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(text = "Uploaded: ${item.uploadDate}", modifier = Modifier)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailScreen(imageUri: String?, navController: NavHostController) {
    val context = LocalContext.current
    if (imageUri == null) {
        Text("No image found.")
        return
    }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Invoice Detail") }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            Image(
                painter = rememberAsyncImagePainter(Uri.decode(imageUri)),
                contentDescription = "Invoice image",
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                removeInvoiceItems(context, listOf(Uri.decode(imageUri)))
                navController.popBackStack()
            }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
                Text("Delete Invoice")
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