package li.songe.gkd.ui

import SectionWrap
import android.app.Activity
import android.content.Context
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blankj.utilcode.util.LogUtils
import com.dylanc.activityresult.launcher.launchForResult
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ActivityLogPageDestination
import com.ramcosta.composedestinations.generated.destinations.SnapshotPageDestination
import com.ramcosta.composedestinations.utils.toDestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import li.songe.gkd.MainActivity
import li.songe.gkd.appScope
import li.songe.gkd.debug.FloatingService
import li.songe.gkd.debug.ScreenshotService
import li.songe.gkd.permission.canDrawOverlaysState
import li.songe.gkd.permission.notificationState
import li.songe.gkd.permission.requiredPermission
import li.songe.gkd.permission.shizukuOkState
import li.songe.gkd.shizuku.shizukuCheckActivity
import li.songe.gkd.shizuku.shizukuCheckUserService
import li.songe.gkd.ui.component.AuthCard
import li.songe.gkd.ui.component.SettingItem
import li.songe.gkd.ui.component.TextSwitch
import li.songe.gkd.ui.component.updateDialogOptions
import li.songe.gkd.ui.style.EmptyHeight
import li.songe.gkd.ui.style.titleItemPadding
import li.songe.gkd.util.LocalNavController
import li.songe.gkd.util.ProfileTransitions
import li.songe.gkd.util.buildLogFile
import li.songe.gkd.util.launchAsFn
import li.songe.gkd.util.launchTry
import li.songe.gkd.util.openUri
import li.songe.gkd.util.privacyStoreFlow
import li.songe.gkd.util.saveFileToDownloads
import li.songe.gkd.util.shareFile
import li.songe.gkd.util.storeFlow
import li.songe.gkd.util.throttle
import li.songe.gkd.util.toast
import rikka.shizuku.Shizuku

@Destination<RootGraph>(style = ProfileTransitions::class)
@Composable
fun AdvancedPage() {
    val context = LocalContext.current as MainActivity
    val vm = viewModel<AdvancedVm>()
    val navController = LocalNavController.current
    val store by storeFlow.collectAsState()
    val snapshotCount by vm.snapshotCountFlow.collectAsState()

    vm.uploadOptions.ShowDialog()


    var showShareLogDlg by remember {
        mutableStateOf(false)
    }
    if (showShareLogDlg) {
        Dialog(onDismissRequest = { showShareLogDlg = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                val modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                Text(
                    text = "分享到其他应用", modifier = Modifier
                        .clickable(onClick = throttle {
                            showShareLogDlg = false
                            vm.viewModelScope.launchTry(Dispatchers.IO) {
                                val logZipFile = buildLogFile()
                                context.shareFile(logZipFile, "分享日志文件")
                            }
                        })
                        .then(modifier)
                )
                Text(
                    text = "保存到下载", modifier = Modifier
                        .clickable(onClick = throttle {
                            showShareLogDlg = false
                            vm.viewModelScope.launchTry(Dispatchers.IO) {
                                val logZipFile = buildLogFile()
                                context.saveFileToDownloads(logZipFile)
                            }
                        })
                        .then(modifier)
                )
                Text(
                    text = "生成链接(需科学上网)",
                    modifier = Modifier
                        .clickable(onClick = throttle {
                            showShareLogDlg = false
                            vm.uploadOptions.startTask(getFile = { buildLogFile() })
                        })
                        .then(modifier)
                )
            }
        }
    }

    var showEditCookieDlg by remember { mutableStateOf(false) }
    if (showEditCookieDlg) {
        val privacyStore by privacyStoreFlow.collectAsState()
        var value by remember {
            mutableStateOf(privacyStore.githubCookie ?: "")
        }
        AlertDialog(
            onDismissRequest = {
                if (value.isEmpty()) {
                    showEditCookieDlg = false
                }
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = "Github Cookie")
                    IconButton(onClick = throttle {
                        context.openUri("https://gkd.li/?r=1")
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                            contentDescription = null,
                        )
                    }
                }
            },
            text = {
                OutlinedTextField(
                    value = value,
                    onValueChange = {
                        value = it.filter { c -> c != '\n' && c != '\r' }
                    },
                    placeholder = { Text(text = "请输入 Github Cookie") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 10,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showEditCookieDlg = false
                    privacyStoreFlow.update { it.copy(githubCookie = value.trim()) }
                }) {
                    Text(text = "确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditCookieDlg = false }) {
                    Text(text = "取消")
                }
            }
        )
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(scrollBehavior = scrollBehavior, navigationIcon = {
                IconButton(onClick = {
                    navController.popBackStack()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                    )
                }
            }, title = { Text(text = "其它设置") }, actions = {})
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding),
        ) {
            SectionWrap(
                title = "Shizuku"
            ) {
                val shizukuOk by shizukuOkState.stateFlow.collectAsState()
                if (!shizukuOk) {
                    AuthCard(title = "Shizuku授权",
                        desc = "高级模式:准确区别界面ID,强制模拟点击",
                        onAuthClick = {
                            try {
                                Shizuku.requestPermission(Activity.RESULT_OK)
                            } catch (e: Exception) {
                                LogUtils.d("Shizuku授权错误", e.message)
                                context.mainVm.shizukuErrorFlow.value = true
                            }
                        })
                    ShizukuFragment(false)
                } else {
                    ShizukuFragment()
                }
            }

            SectionWrap(
                title = "快照"
            ) {

                SettingItem(
                    title = "快照记录" + (if (snapshotCount > 0) "-$snapshotCount" else ""),
                    onClick = {
                        navController.toDestinationsNavigator().navigate(SnapshotPageDestination)
                    }
                )

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    val screenshotRunning by ScreenshotService.isRunning.collectAsState()
                    TextSwitch(
                        title = "截屏服务",
                        subtitle = "生成快照需要获取屏幕截图",
                        checked = screenshotRunning,
                        onCheckedChange = vm.viewModelScope.launchAsFn<Boolean> {
                            if (it) {
                                requiredPermission(context, notificationState)
                                val mediaProjectionManager =
                                    context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                                val activityResult =
                                    context.launcher.launchForResult(mediaProjectionManager.createScreenCaptureIntent())
                                if (activityResult.resultCode == Activity.RESULT_OK && activityResult.data != null) {
                                    ScreenshotService.start(intent = activityResult.data!!)
                                }
                            } else {
                                ScreenshotService.stop()
                            }
                        }
                    )
                }

                val floatingRunning by FloatingService.isRunning.collectAsState()
                TextSwitch(
                    title = "悬浮窗服务",
                    subtitle = "显示悬浮按钮点击保存快照",
                    checked = floatingRunning,
                    onCheckedChange = vm.viewModelScope.launchAsFn<Boolean> {
                        if (it) {
                            requiredPermission(context, notificationState)
                            requiredPermission(context, canDrawOverlaysState)
                            FloatingService.start()
                        } else {
                            FloatingService.stop()
                        }
                    }
                )

                TextSwitch(
                    title = "音量快照",
                    subtitle = "音量变化时保存快照",
                    checked = store.captureVolumeChange
                ) {
                    storeFlow.value = store.copy(
                        captureVolumeChange = it
                    )
                }

                TextSwitch(
                    title = "截屏快照",
                    subtitle = "触发截屏时保存快照",
                    suffix = "查看限制",
                    onSuffixClick = {
                        context.mainVm.dialogFlow.updateDialogOptions(
                            title = "限制说明",
                            text = "仅支持部分小米设备截屏触发\n\n只保存节点信息不保存图片, 用户需要在快照记录里替换截图",
                        )
                    },
                    checked = store.captureScreenshot
                ) {
                    storeFlow.value = store.copy(
                        captureScreenshot = it
                    )
                }

                TextSwitch(
                    title = "隐藏状态栏",
                    subtitle = "隐藏截图顶部状态栏",
                    checked = store.hideSnapshotStatusBar
                ) {
                    storeFlow.value = store.copy(
                        hideSnapshotStatusBar = it
                    )
                }

                TextSwitch(
                    title = "保存提示",
                    subtitle = "保存时提示\"正在保存快照\"",
                    checked = store.showSaveSnapshotToast
                ) {
                    storeFlow.value = store.copy(
                        showSaveSnapshotToast = it
                    )
                }

                SettingItem(
                    title = "Github Cookie",
                    subtitle = "生成快照/日志链接",
                    suffix = "获取教程",
                    onSuffixClick = {
                        context.openUri("https://gkd.li/?r=1")
                    },
                    imageVector = Icons.Default.Edit,
                    onClick = {
                        showEditCookieDlg = true
                    }
                )
            }

            SectionWrap(
                title = "界面记录"
            ) {
                TextSwitch(
                    title = "记录界面",
                    subtitle = "记录打开的应用及界面",
                    checked = store.enableActivityLog
                ) {
                    storeFlow.value = store.copy(
                        enableActivityLog = it
                    )
                }
                SettingItem(
                    title = "界面记录",
                    onClick = {
                        navController.toDestinationsNavigator().navigate(ActivityLogPageDestination)
                    }
                )
            }

            SectionWrap(
                title = "日志"
            ) {
                TextSwitch(
                    title = "保存日志",
                    subtitle = "保存7天日志,帮助定位BUG",
                    checked = store.log2FileSwitch,
                    onCheckedChange = {
                        storeFlow.value = store.copy(
                            log2FileSwitch = it
                        )
                        if (!it) {
                            context.mainVm.viewModelScope.launchTry(Dispatchers.IO) {
                                val logFiles = LogUtils.getLogFiles()
                                if (logFiles.isNotEmpty()) {
                                    logFiles.forEach { f ->
                                        f.delete()
                                    }
                                    toast("已删除全部日志")
                                }
                            }
                        }
                    })

                if (store.log2FileSwitch) {
                    SettingItem(
                        title = "导出日志",
                        imageVector = Icons.Default.Share,
                        onClick = {
                            showShareLogDlg = true
                        }
                    )
                }
            }

            SectionWrap(
                title = "其它",
            ) {
                TextSwitch(title = "前台悬浮窗",
                    subtitle = "添加透明悬浮窗,关闭可能导致不点击/点击缓慢",
                    checked = store.enableAbFloatWindow,
                    onCheckedChange = {
                        storeFlow.value = store.copy(
                            enableAbFloatWindow = it
                        )
                    })
            }

            Spacer(modifier = Modifier.height(EmptyHeight))
        }
    }

}

@Composable
private fun ShizukuFragment(enabled: Boolean = true) {
    val store by storeFlow.collectAsState()
    TextSwitch(
        title = "Shizuku-界面识别",
        subtitle = "更准确识别界面ID",
        checked = store.enableShizukuActivity,
        enabled = enabled,
        onCheckedChange = appScope.launchAsFn<Boolean>(Dispatchers.IO) {
            if (it) {
                toast("检测中")
                if (!shizukuCheckActivity()) {
                    toast("检测失败,无法使用")
                    return@launchAsFn
                }
                toast("已启用")
            }
            storeFlow.value = store.copy(
                enableShizukuActivity = it
            )
        })

    TextSwitch(
        title = "Shizuku-模拟点击",
        subtitle = "变更 clickCenter 为强制模拟点击",
        checked = store.enableShizukuClick,
        enabled = enabled,
        onCheckedChange = appScope.launchAsFn<Boolean>(Dispatchers.IO) {
            if (it) {
                toast("检测中")
                if (!shizukuCheckUserService()) {
                    toast("检测失败,无法使用")
                    return@launchAsFn
                }
                toast("已启用")
            }
            storeFlow.value = store.copy(
                enableShizukuClick = it
            )

        })

}
