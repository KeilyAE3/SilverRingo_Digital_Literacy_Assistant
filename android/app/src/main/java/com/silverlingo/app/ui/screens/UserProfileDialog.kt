package com.silverlingo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.silverlingo.app.data.UserProfile
import com.silverlingo.app.ui.theme.*

@Composable
fun UserProfileDialog(
    currentProfile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (UserProfile) -> Unit
) {
    var name by remember { mutableStateOf(currentProfile.name) }
    var birth by remember { mutableStateOf(currentProfile.birthDate) }
    var address by remember { mutableStateOf(currentProfile.address) }
    var account by remember { mutableStateOf(currentProfile.accountInfo) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "👤 내 기본 정보 등록",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )
                Text(
                    text = "한 번만 입력해 두시면 서류 뗄 때 자동으로 채워집니다. 서버에 전송되지 않고 스마트폰에만 안전하게 저장됩니다.",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                // 성함
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("성함 (이름)", fontSize = 16.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 생년월일
                OutlinedTextField(
                    value = birth,
                    onValueChange = { if (it.length <= 6) birth = it },
                    label = { Text("생년월일 (앞 6자리, 예: 520315)", fontSize = 16.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 주소
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("주소 (예: 서울시 종로구...)", fontSize = 16.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 계좌번호
                OutlinedTextField(
                    value = account,
                    onValueChange = { account = it },
                    label = { Text("연금/환급 계좌 (예: 국민은행 123-456)", fontSize = 16.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("취소", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            onSave(
                                UserProfile(
                                    name = name.trim(),
                                    birthDate = birth.trim(),
                                    address = address.trim(),
                                    accountInfo = account.trim()
                                )
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                    ) {
                        Text("저장하기", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AmberAccent)
                    }
                }
            }
        }
    }
}
