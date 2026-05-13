package com.example.gwt.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gwt.R
import com.example.gwt.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WasteGuideScreen(
    onBack: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(stringResource(R.string.guide_title), fontWeight = FontWeight.ExtraBold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { authViewModel.signOut() },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = stringResource(R.string.logout))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                ),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text(
                    "Separate your waste effectively to help our Panchayat maintain a clean and green environment.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
            }
            
            item {
                WasteCategoryCard(
                    title = stringResource(R.string.wet_waste),
                    subTitle = "Biodegradable Items",
                    color = Color(0xFF2E7D32),
                    icon = Icons.Default.Eco, 
                    items = listOf("Vegetable & Fruit peels", "Food scraps", "Tea & Coffee waste", "Garden waste & Flowers")
                )
            }
            
            item {
                WasteCategoryCard(
                    title = stringResource(R.string.dry_waste),
                    subTitle = "Recyclable Items",
                    color = Color(0xFF1565C0),
                    icon = Icons.Default.Inventory,
                    items = listOf("Plastic bottles & covers", "Paper, Books & Cardboard", "Glass & Metal items", "Rubber & Cloth scraps")
                )
            }
            
            item {
                WasteCategoryCard(
                    title = stringResource(R.string.hazardous_waste),
                    subTitle = "Dangerous Items",
                    color = Color(0xFFC62828),
                    icon = Icons.Default.Warning,
                    items = listOf("Batteries & Bulbs", "Expired medicines", "Paints & Chemicals", "Electronic parts")
                )
            }
        }
    }
}

@Composable
fun WasteCategoryCard(
    title: String, 
    subTitle: String, 
    color: Color, 
    icon: ImageVector,
    items: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = color.copy(alpha = 0.1f)
                ) {
                    Icon(
                        imageVector = icon, 
                        contentDescription = null, 
                        tint = color,
                        modifier = Modifier.padding(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = title, style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.ExtraBold)
                    Text(text = subTitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(20.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle, 
                            contentDescription = null, 
                            modifier = Modifier.size(18.dp),
                            tint = color.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(text = item, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}
