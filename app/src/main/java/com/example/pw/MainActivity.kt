package com.example.pw

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pw.ui.theme.PwTheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    private var keepScreenOnJob: Job? = null

    fun keepScreenOnTemporarily(durationMs: Long = 20000) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        keepScreenOnJob?.cancel()
        keepScreenOnJob = lifecycleScope.launch {
            delay(durationMs)
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PwTheme {
                val navController = rememberNavController()
                val initialUser = remember { Firebase.auth.currentUser }
                var currentUser by remember { mutableStateOf(initialUser) }

                // Listen for Auth changes
                DisposableEffect(Unit) {
                    val listener = FirebaseAuth.AuthStateListener { auth ->
                        val user = auth.currentUser
                        if (user?.uid != currentUser?.uid) {
                            Log.d("MainActivity", "Auth state changed: user=${user?.uid}")
                            currentUser = user
                            
                            // Navigate only when state actually changes from the initial/current state
                            if (user != null) {
                                navController.navigate("main") {
                                    popUpTo("auth") { inclusive = true }
                                }
                            } else {
                                navController.navigate("auth") {
                                    popUpTo("main") { inclusive = true }
                                }
                            }
                        }
                    }
                    Firebase.auth.addAuthStateListener(listener)
                    onDispose { Firebase.auth.removeAuthStateListener(listener) }
                }

                NavHost(
                    navController = navController,
                    startDestination = if (initialUser == null) "auth" else "main"
                ) {
                    composable("auth") {
                        AuthScreen()
                    }
                    composable("main") {
                        val user = currentUser
                        if (user != null) {
                            MainScreen(user = user)
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuthScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = androidx.credentials.CredentialManager.create(context)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Welcome to Password Manager", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = {
            scope.launch {
                try {
                    Log.d("AuthScreen", "Sign-in button clicked")
                    val rawNonce = UUID.randomUUID().toString()
                    val bytes = rawNonce.toByteArray()
                    val md = MessageDigest.getInstance("SHA-256")
                    val digest = md.digest(bytes)
                    val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

                    val googleIdOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                        .setNonce(hashedNonce)
                        .setAutoSelectEnabled(true)
                        .build()

                    val request = androidx.credentials.GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()

                    Log.d("AuthScreen", "Requesting credentials...")
                    val result = credentialManager.getCredential(context, request)
                    val credential = result.credential
                    Log.d("AuthScreen", "Received credential of type: ${credential.type}")

                    val googleIdTokenCredential = try {
                        if (credential is GoogleIdTokenCredential) {
                            credential
                        } else {
                            GoogleIdTokenCredential.createFrom(credential.data)
                        }
                    } catch (e: Exception) {
                        Log.e("AuthScreen", "Failed to parse Google ID Token", e)
                        null
                    }

                    if (googleIdTokenCredential != null) {
                        Log.d("AuthScreen", "ID Token extracted successfully")
                        Toast.makeText(context, "Signing into Firebase...", Toast.LENGTH_SHORT).show()
                        val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                        Firebase.auth.signInWithCredential(firebaseCredential).await()
                    } else {
                        Log.e("AuthScreen", "Could not extract Google ID Token from ${credential.type}")
                        Toast.makeText(context, "Sign-in error: Invalid response from Google", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Log.e("AuthScreen", "Sign-in failed", e)
                    Toast.makeText(context, "Sign-in failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }) {
            Text("Sign in with Google")
        }
    }
}

@Composable
fun MainScreen(user: FirebaseUser) {
    val dbRef = Firebase.database.reference.child("users").child(user.uid).child("passwords")
    var passwords by remember { mutableStateOf(listOf<PwEntity>()) }
    var searchQuery by remember { mutableStateOf("") }
    
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    
    var entryToEdit by remember { mutableStateOf<PwEntity?>(null) }
    var entryToDelete by remember { mutableStateOf<PwEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    DisposableEffect(user.uid) {
        Log.d("MainScreen", "Setting up database listener for user: ${user.uid}")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<PwEntity>()
                for (child in snapshot.children) {
                    val entity = child.getValue(PwEntity::class.java)
                    if (entity != null) {
                        list.add(entity.copy(id = child.key))
                    }
                }
                passwords = list
                Log.d("MainScreen", "Data updated: ${list.size} items")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainScreen", "Firebase error", error.toException())
            }
        }
        dbRef.addValueEventListener(listener)
        onDispose { 
            Log.d("MainScreen", "Removing database listener")
            dbRef.removeEventListener(listener) 
        }
    }

    val filteredPasswords = remember(passwords, searchQuery) {
        if (searchQuery.isBlank()) {
            passwords
        } else {
            passwords.filter { 
                it.vendor.contains(searchQuery, ignoreCase = true) || 
                it.account.contains(searchQuery, ignoreCase = true) 
            }
        }
    }

    if (showAddDialog) {
        EntryDialog(
            title = "Add Password Entry",
            onDismiss = { showAddDialog = false },
            onSave = { vendor, account, password ->
                val newEntryRef = dbRef.push()
                val entry = PwEntity(vendor = vendor, account = account, pw = password)
                newEntryRef.setValue(entry)
                showAddDialog = false
            }
        )
    }

    entryToEdit?.let { entity ->
        EntryDialog(
            title = "Edit Password Entry",
            initialVendor = entity.vendor,
            initialAccount = entity.account,
            initialPassword = entity.pw,
            onDismiss = { entryToEdit = null },
            onSave = { vendor, account, password ->
                entity.id?.let { id ->
                    dbRef.child(id).setValue(PwEntity(vendor = vendor, account = account, pw = password))
                }
                entryToEdit = null
            }
        )
    }

    entryToDelete?.let { entity ->
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text("Confirm Delete") },
            text = { Text("Are you sure you want to delete the entry for '${entity.vendor}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        entity.id?.let { id ->
                            dbRef.child(id).removeValue()
                        }
                        entryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF2196F3), // Standard Blue
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add New Entry")
            }
        }
    ) { paddingValues ->
        Surface(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Passwords",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    TextButton(onClick = { 
                        Log.d("MainScreen", "Sign out clicked")
                        Firebase.auth.signOut() 
                    }) {
                        Text("Sign Out")
                    }
                }

                Text(text = "Logged in as: ${user.email}", style = MaterialTheme.typography.bodySmall)

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search vendors or accounts...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn {
                    items(filteredPasswords, key = { it.id ?: "" }) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.vendor,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    if (item.account.isNotBlank()) {
                                        Text(
                                            text = item.account,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Row {
                                    IconButton(onClick = {
                                        clipboardManager.setText(AnnotatedString(item.pw))
                                        Toast.makeText(context, "${item.vendor}'s password has been copied", Toast.LENGTH_SHORT).show()
                                        (context as? MainActivity)?.keepScreenOnTemporarily()
                                    }) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Password")
                                    }
                                    IconButton(onClick = { entryToEdit = item }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                                    }
                                    IconButton(onClick = { entryToDelete = item }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
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

@Composable
fun EntryDialog(
    title: String,
    initialVendor: String = "",
    initialAccount: String = "",
    initialPassword: String = "",
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var vendor by remember { mutableStateOf(initialVendor) }
    var account by remember { mutableStateOf(initialAccount) }
    var password by remember { mutableStateOf(initialPassword) }
    var originalPassword by remember { mutableStateOf(initialPassword) }

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = vendor,
                    onValueChange = { vendor = it },
                    label = { Text("Vendor (e.g. Google)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = account,
                    onValueChange = { account = it },
                    label = { Text("Account Name/User") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Show Original Password only if we are editing (title contains "Edit")
                if (title.contains("Edit", ignoreCase = true)) {
                    OutlinedTextField(
                        value = originalPassword,
                        onValueChange = { originalPassword = it },
                        label = { Text("Original Password") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = {
                                clipboardManager.setText(AnnotatedString(originalPassword))
                                Toast.makeText(context, "Original password copied", Toast.LENGTH_SHORT).show()
                                (context as? MainActivity)?.keepScreenOnTemporarily()
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Original Password")
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        Row {
                            IconButton(onClick = {
                                clipboardManager.setText(AnnotatedString(password))
                                Toast.makeText(context, "Password copied", Toast.LENGTH_SHORT).show()
                                (context as? MainActivity)?.keepScreenOnTemporarily()
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Password")
                            }
                            IconButton(onClick = { password = generateStrongPassword() }) {
                                Icon(Icons.Default.VpnKey, contentDescription = "Generate Password")
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { password = generateStrongPassword() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.VpnKey, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Generate Strong Password")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (vendor.isNotBlank() && password.isNotBlank()) onSave(vendor, account, password) },
                enabled = vendor.isNotBlank() && password.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun generateStrongPassword(length: Int = 12): String {
    val charPool = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+"
    return (1..length)
        .map { charPool.random() }
        .joinToString("")
}
