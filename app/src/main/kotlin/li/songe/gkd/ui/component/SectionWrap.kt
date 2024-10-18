import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import li.songe.gkd.ui.style.EmptyHeight
import li.songe.gkd.ui.style.titleItemPadding

@Composable
fun SectionWrap(
    title: String,
    content: @Composable () -> Unit // Lambda to pass custom content
) {
    Text(
        text = title,
        modifier = Modifier.titleItemPadding(),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )

    content()

    Spacer(modifier = Modifier.height(EmptyHeight))
}