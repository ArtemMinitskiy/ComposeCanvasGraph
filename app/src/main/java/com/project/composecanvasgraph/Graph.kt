package com.project.composecanvasgraph

import android.graphics.PointF
import android.text.TextPaint
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.composepractice.MainViewModel
import com.example.composepractice.noRippleClickable
import com.project.composecanvasgraph.ui.theme.TransparentGrey
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.roundToInt

@Composable
fun Graph() {
    val viewModelMain: MainViewModel = viewModel()
    val scrollState = rememberScrollState()

    val controlPoints1 = mutableListOf<PointF>()
    val controlPoints2 = mutableListOf<PointF>()
    val coordinates = mutableListOf<PointF>()

    val yPoints = (1..100).toList()
    val xPoints = (0..60).toList()  //Time in seconds from 1 to 60
    val trigger = remember { mutableStateOf(false) }
    val isLineCurved = remember { mutableStateOf(false) }
    val xStepScroll = remember { mutableStateOf(0f) }
    val xStepConst = remember { mutableStateOf(0) }
    val sizeWidth = remember { mutableStateOf(0f) }
    val count = remember { mutableStateOf(0) }
//    val points = List(60) { Random.nextInt(0, 100) }
//    val points = listOf(2, 21, 68, 57, 78, 91, 31, 92, 63, 76, 2, 18, 46, 16, 66, 88, 49, 51, 40, 7, 55, 30, 37, 23, 47, 9, 30, 46, 37, 5, 75, 61, 44, 10, 9, 33, 11, 41, 23, 96, 91, 37, 70, 9, 89, 52, 54, 2, 22, 65, 67, 61, 71, 16, 45, 91, 75, 97, 74, 53)
    val points = remember { mutableListOf<Int>() }

    val stepCoof = remember { mutableStateOf(1) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .weight(1f)
        ) {
            key(count.value) {
                Canvas(
                    modifier = Modifier
                        .padding(top = 100.dp)
                        .padding(start = 20.dp)
                        .horizontalScroll(scrollState)
                        .width((xPoints.size * 20 * stepCoof.value).dp)  // Adjust width for scrolling
                        .height(400.dp)  // Height to fit graph and labels
                ) {
                    // Draw Y-axis (1-100 with labels every 20)
                    val xStep = size.width / (xPoints.size - 1)
                    val yStep = size.height / 5  // 5 intervals (0, 20, 40, 60, 80, 100)
                    xStepConst.value = xStep.toInt()
                    for (i in 0..5) {
                        val yValue = 100 - (i * 20)
                        val yPosition = i * yStep
                        drawLine(
                            color = if (yValue == 0) Color.Black else Color.Gray,
                            start = Offset(0f + 20f, yPosition),
                            end = Offset(size.width, yPosition),
                            strokeWidth = if (yValue == 0) 3f else 2f
                        )
                    }

                    // Draw X-axis (Time 1-60 seconds)
                    for (i in xPoints.indices) {
                        val xValue = xPoints[i]
                        val xPosition = i * xStep

                        // Draw vertical grid line for each second
                        drawLine(
                            color = if (xValue == 0) Color.Black else Color.Gray,
                            start = Offset(if (xValue == 0) xStep else xPosition, 0f),
                            end = Offset(if (xValue == 0) xStep else xPosition, size.height),
                            strokeWidth = if (xValue == 0) 3f else 2f
                        )

                        // Draw X-axis labels (seconds)
                        drawContext.canvas.nativeCanvas.drawText(
                            "$xValue",
                            xPosition,
                            size.height + 30f,  // Adjust to be near the bottom of the canvas
                            TextPaint().apply {
                                color = android.graphics.Color.BLACK
                                textSize = 32f
                            }
                        )
                    }

                    if (points.isNotEmpty()) {
                        for (i in points.indices) {
                            val x1 = xStep * xPoints[i]
                            val y1 = size.height - (yStep * (points[i] / 20.toFloat()))
                            coordinates.add(PointF(x1, y1))
                            /*if (graphAppearance.isCircleVisible) {
                                drawCircle(
                                    color = graphAppearance.circleColor,
                                    radius = 10f,
                                    center = Offset(x1, y1)
                                )
                            }*/
                        }
                        for (i in 1 until coordinates.size) {
                            controlPoints1.add(PointF((coordinates[i].x + coordinates[i - 1].x) / 2, coordinates[i - 1].y))
                            controlPoints2.add(PointF((coordinates[i].x + coordinates[i - 1].x) / 2, coordinates[i].y))
                        }

                        val curvedLine = Path().apply {
                            reset()
                            moveTo(coordinates.first().x, coordinates.first().y)
                            for (i in 0 until coordinates.size - 1) {
                                cubicTo(
                                    controlPoints1[i].x, controlPoints1[i].y,
                                    controlPoints2[i].x, controlPoints2[i].y,
                                    coordinates[i + 1].x, coordinates[i + 1].y
                                )
                            }
                        }
                        val simpleLine = Path().apply {
                            points.forEachIndexed { index, value ->
//                                val x = index * xStep  // Scale X to match seconds
                                val x = index * xStep  // Scale X to match seconds
                                val y = size.height - (value / 100f) * size.height  // Scale Y values from 1-100
                                if (index == 0) moveTo(x, y) else lineTo(x, y)
                            }
                        }

                        if (isLineCurved.value) {
                            drawPath(
                                curvedLine,
                                color = Color.Blue,
                                style = Stroke(
                                    width = 5f,
                                    cap = StrokeCap.Round
                                )
                            )
                        } else {
                            drawPath(
                                path = simpleLine,
                                color = Color.Blue,
                                style = Stroke(
                                    width = 5f
                                )
                            )
                        }
                    }
                }
            }

            Canvas(
                modifier = Modifier
                    .padding(top = 100.dp)
                    .padding(start = 20.dp)
                    .width((xPoints.size * 20).dp)  // Adjust width for scrolling
                    .height(400.dp)  // Height to fit graph and labels
            ) {

                sizeWidth.value = size.width

                // Draw Y-axis (1-100 with labels every 20)
                val xStep = size.width / (xPoints.size - 1)
                val yStep = size.height / 5  // 5 intervals (0, 20, 40, 60, 80, 100)
                for (i in 0..5) {
                    val yValue = 100 - (i * 20)
                    val yPosition = i * yStep
                    // Draw Y-axis labels
                    if (yValue > 0) {
                        drawContext.canvas.nativeCanvas.drawText(
                            "$yValue",
                            -20f,
                            yPosition + 15f,  // Offset to adjust label position
                            TextPaint().apply {
                                color = android.graphics.Color.BLACK
                                textSize = 32f
                            }
                        )
                    }
                }

                // Draw X-axis (Time 1-60 seconds)
                for (i in xPoints.indices) {
                    val xValue = xPoints[i]
//                    val xPosition = i * xStep

                    if (xValue == 0) {
                        drawLine(
                            color = Color.Black,
                            start = Offset(xStep, 0f),
                            end = Offset(xStep, size.height),
                            strokeWidth = 3f
                        )
                    }
                }
            }

            SizeChangeSliderBox(sizeWidth, xStepConst) {
                when (it) {
                    in 0..33 -> stepCoof.value = 1
                    in 31..66 -> stepCoof.value = 2
                    in 67..100 -> stepCoof.value = 3
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            //Trigger
            Box(
                Modifier
                    .weight(1f)
                    .height(60.dp)
                    .background(Color.DarkGray, RoundedCornerShape(8.dp))
                    .padding(8.dp)
                    .noRippleClickable {
                        trigger.value = true
                    },
            ) {
                Text(
                    modifier = Modifier
                        .align(Alignment.Center),
                    text = stringResource(R.string.trigger),
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Box(
                Modifier
                    .weight(1f)
                    .height(60.dp)
                    .background(Color.DarkGray, RoundedCornerShape(8.dp))
                    .padding(8.dp)
                    .noRippleClickable {
                        isLineCurved.value = !isLineCurved.value
                    },
            ) {
                Text(
                    modifier = Modifier
                        .align(Alignment.Center),
                    text = if (!isLineCurved.value) stringResource(R.string.curved_line) else stringResource(R.string.line),
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        LaunchedEffect(trigger.value) {
            if (trigger.value) {
                scrollState.animateScrollTo(scrollState.maxValue)
                trigger.value = false
            }
        }
    }

    LaunchedEffect(viewModelMain) {
        viewModelMain.testPointFlow.collectLatest {
            Log.e("mLogTestRotation", "Point: $it")
            points.add(it)
//            Log.i("mLogTestRotation", "Points: $points")
            count.value++

//            if (points.size > 16) {
//            xStepScroll.value += xStepConst.value
//            Log.i("mLogTestRotation", "xStepScroll: ${xStepScroll.value}")
//            if (!scrollState.isScrollInProgress) {
//                delay(100)
//                scrollState.animateScrollBy(xStepScroll.value)
//            }
//            }
        }
    }

    /*LaunchedEffect(xStepScroll.value) {
        Log.i("mLogTestRotation", "${xStepScroll.value}")
        if (!scrollState.isScrollInProgress) {
            delay(100)
            scrollState.animateScrollBy(xStepScroll.value)
        }
    }*/

}

@Composable
private fun SizeChangeSliderBox(
    sizeWidth: MutableState<Float>, xStepConst: MutableState<Int>, percent: (Int) -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }

    // Get screen width in pixels
    val screenWidthPx = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }

    // Calculate the width of the text (approximation) or manually define it
    val textWidthPx = with(LocalDensity.current) { 100.dp.toPx() } // Approximate text width

    Box(
        modifier = Modifier
            .padding(top = 100.dp)
            .offset { IntOffset(offsetX.roundToInt() + xStepConst.value, 0) }
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
//                    Log.e("mLogTest", "offsetX: $offsetX")
//                    Log.e("mLogTest", "sizeWidth: ${sizeWidth.value}")
//                    Log.i("mLogTest", "%: ${(offsetX * 100) / sizeWidth.value}")

                    // Calculate new offset and constrain it within screen boundaries
                    val newOffsetX = offsetX + delta

                    // Ensure text doesn't go beyond the screen's left or right bounds
                    offsetX = newOffsetX.coerceIn(0f, screenWidthPx - textWidthPx)
                    percent(((offsetX * 100) / sizeWidth.value).toInt())
                }
            )
            .height(400.dp)
            .width(20.dp)
            .background(TransparentGrey),
    )
}
