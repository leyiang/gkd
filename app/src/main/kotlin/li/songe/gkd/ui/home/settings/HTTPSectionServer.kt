import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import li.songe.gkd.debug.HttpService
import li.songe.gkd.permission.notificationState
import li.songe.gkd.permission.requiredPermission
import li.songe.gkd.ui.component.SettingItem
import li.songe.gkd.ui.component.TextSwitch
import li.songe.gkd.ui.style.itemPadding
import li.songe.gkd.ui.style.titleItemPadding
import li.songe.gkd.util.launchAsFn
import li.songe.gkd.util.openUri
import li.songe.gkd.util.storeFlow
import li.songe.gkd.util.throttle
import li.songe.gkd.util.toast
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import li.songe.gkd.MainActivity
import li.songe.gkd.ui.home.HomeVm

@Composable
fun SettingHTTPServerSection() {
    val store by storeFlow.collectAsState()
    val vm = viewModel<HomeVm>()
    val context = LocalContext.current as MainActivity

    var showEditPortDlg by remember {
        mutableStateOf(false)
    }
    if (showEditPortDlg) {
        var value by remember {
            mutableStateOf(store.httpServerPort.toString())
        }
        AlertDialog(title = { Text(text = "服务端口") }, text = {
            OutlinedTextField(
                value = value,
                placeholder = {
                    Text(text = "请输入 5000-65535 的整数")
                },
                onValueChange = {
                    value = it.filter { c -> c.isDigit() }.take(5)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = {
                    Text(
                        text = "${value.length} / 5",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                    )
                },
            )
        }, onDismissRequest = {
            if (value.isEmpty()) {
                showEditPortDlg = false
            }
        }, confirmButton = {
            TextButton(
                enabled = value.isNotEmpty(),
                onClick = {
                    val newPort = value.toIntOrNull()
                    if (newPort == null || !(5000 <= newPort && newPort <= 65535)) {
                        toast("请输入 5000-65535 的整数")
                        return@TextButton
                    }
                    storeFlow.value = store.copy(
                        httpServerPort = newPort
                    )
                    showEditPortDlg = false
                }
            ) {
                Text(
                    text = "确认", modifier = Modifier
                )
            }
        }, dismissButton = {
            TextButton(onClick = { showEditPortDlg = false }) {
                Text(
                    text = "取消"
                )
            }
        })
    }
    val httpServerRunning by HttpService.isRunning.collectAsState()
    val localNetworkIps by HttpService.localNetworkIpsFlow.collectAsState()

    Text(
        text = "HTTP服务",
        modifier = Modifier.titleItemPadding(),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
    Row(
        modifier = Modifier.itemPadding(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "HTTP服务",
                style = MaterialTheme.typography.bodyLarge,
            )
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.bodyMedium
            ) {
                if (!httpServerRunning) {
                    Text(
                        text = "在浏览器下连接调试工具",
                    )
                } else {
                    Text(
                        text = "点击下面任意链接打开即可自动连接",
                    )
                    Row {
                        Text(
                            text = "http://127.0.0.1:${store.httpServerPort}",
                            color = MaterialTheme.colorScheme.primary,
                            style = LocalTextStyle.current.copy(textDecoration = TextDecoration.Underline),
                            modifier = Modifier.clickable(onClick = throttle {
                                context.openUri("http://127.0.0.1:${store.httpServerPort}")
                            }),
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(text = "仅本设备可访问")
                    }
                    localNetworkIps.forEach { host ->
                        Text(
                            text = "http://${host}:${store.httpServerPort}",
                            color = MaterialTheme.colorScheme.primary,
                            style = LocalTextStyle.current.copy(textDecoration = TextDecoration.Underline),
                            modifier = Modifier.clickable(onClick = throttle {
                                context.openUri("http://${host}:${store.httpServerPort}")
                            })
                        )
                    }
                }
            }
        }
        Switch(
            checked = httpServerRunning,
            onCheckedChange = vm.viewModelScope.launchAsFn<Boolean> {
                if (it) {
                    requiredPermission(context, notificationState)
                    HttpService.start()
                } else {
                    HttpService.stop()
                }
            }
        )
    }

    SettingItem(
        title = "服务端口",
        subtitle = store.httpServerPort.toString(),
        imageVector = Icons.Default.Edit,
        onClick = {
            showEditPortDlg = true
        }
    )

    TextSwitch(
        title = "清除订阅",
        subtitle = "服务关闭时,删除内存订阅",
        checked = store.autoClearMemorySubs
    ) {
        storeFlow.value = store.copy(
            autoClearMemorySubs = it
        )
    }
}