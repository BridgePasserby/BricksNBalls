package com.zingking.bricks.mvp.presenter;

import android.graphics.PointF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.zingking.bricks.entity.BackgroundPosition;
import com.zingking.bricks.entity.MathPoint;
import com.zingking.bricks.mvp.model.BrickModel;
import com.zingking.bricks.mvp.view.IBrickView;
import com.zingking.bricks.widget.BricksBackgroundView;

/**
 * Copyright (c) 2019, Z.kai All rights reserved.
 *
 * @author Z.kai
 * @date 2019/3/14
 * @description
 */
public class BrickPresenter implements IBrickPresenter {
    private static final String TAG = "BrickPresenter";
    private final Object object = new Object();
    private final BrickModel brickModel;
    private boolean isDrawing = false;
    private double moveY = 0;
    private double moveX = 0;
    /**
     * 小球每次移动的距离
     */
    private float delta = 10f;
    private float ballRadius = 20f;
    private IBrickView iBrickView;
    /**
     * 计算小球移动的线程
     */
    private Thread ballMoveThread;
    private boolean isRun = false;
    private boolean isRight = true;
    private boolean isDown = true;

    public BrickPresenter(IBrickView iBrickView) {
        this.iBrickView = iBrickView;
        brickModel = new BrickModel();
    }

    @Override
    public void createLevel() {
        iBrickView.addBackgroundView(new BricksBackgroundView.IDrawListener() {
            @Override
            public void onSuccess(BackgroundPosition position) {
                brickModel.createLevel();
                brickModel.setBackgroundPosition(position);
                drawBrickByLevel(position.getXPositions(), position.getYPositions());
                iBrickView.addLineView();
                iBrickView.addBallView();
            }

            @Override
            public void onFailed() {

            }
        });
    }

    @Override
    public void onBackgroundTouch(View view, MotionEvent event) {
        Log.d(TAG, "dispatchTouchEvent() called with: event = [" + event + "]");
        if (isDrawing) {
            return;
        }
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                float x = event.getX();
                float y = event.getY();
                updateLine(false, new PointF(x, y));
                break;
            case MotionEvent.ACTION_MOVE:
                x = event.getX();
                y = event.getY();
                updateLine(false, new PointF(x, y));
                break;
            case MotionEvent.ACTION_OUTSIDE:
                Log.i(TAG, "dispatchTouchEvent: 超界");
                break;
            case MotionEvent.ACTION_UP:
                    startAutoMove();
                break;
            default:
                break;
        }
    }

    @Override
    public void removeBrick(MathPoint mathPoint) {
        brickModel.removeMathPoint(mathPoint);
    }

    private void updateLine(boolean isStart, PointF pointF) {
        if (isStart) {
            brickModel.setLineStartPF(pointF);
        } else {
            brickModel.setLineStopPF(pointF);
        }
        iBrickView.updateLine(isStart, pointF);
    }

    private void startAutoMove() {
        double angle = brickModel.getAngle();
        moveY = Math.sin(angle * Math.PI / 180) * delta;
        moveX = Math.cos(angle * Math.PI / 180) * delta;
        if (isDrawing) {
            return;
        }
        isRight = angle > 0;
        isDown = true;
        moveY = Math.abs(moveY);
        moveX = Math.abs(moveX);
        final PointF pointF = new PointF(ballRadius, ballRadius);
        synchronized (object) {
            object.notify();
        }
        if (ballMoveThread == null) {
            ballMoveThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            Log.i(TAG, "run: pointF =  " + pointF.toString());
                            if (pointF.y - ballRadius < 0) {
                                synchronized (object) {
                                    isDrawing = false;
                                    updateLine(true, pointF);
                                    pointF.y = ballRadius;
                                    object.wait();
                                }
                            }
                            Thread.sleep(2);
                            boolean changeLR = brickModel.changeLR(isRight, pointF);
                            boolean changeTB = brickModel.changeTB(isDown, pointF);
                            if (isRight) {
                                if (changeLR) {
                                    isRight = false;
                                } else {
                                    pointF.x += moveX;
                                }
                            } else {
                                if (changeLR) {
                                    isRight = true;
                                } else {
                                    pointF.x -= moveX;
                                }
                            }
                            if (isDown) {
                                if (changeTB) {
                                    isDown = false;
                                } else {
                                    pointF.y += moveY;
                                }
                            } else {
                                if (changeTB) {
                                    isDown = true;
                                } else {
                                    pointF.y -= moveY;
                                }
                            }
                            int[] ballPosition = brickModel.getBallPosition(pointF);
                            iBrickView.updateBall(pointF);
                            iBrickView.updateBallCoordinate(ballPosition);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
        if (!isRun) {
            ballMoveThread.start();
            isRun = true;
        }
        isDrawing = true;
    }

    private void drawBrickByLevel(final float[] xPositions, final float[] yPositions) {
        int[][] brickPosition = brickModel.getBrickPosition();
        int yNum = yPositions.length;// y轴砖块数据点的数量
        int xNum = xPositions.length;// x轴砖块数据点的数量
        // 遍历砖块样式
        for (int yCoordinate = 0, yLen = brickPosition.length; yCoordinate < yLen; yCoordinate++) {
            if (yNum - 1 != yLen) {
                Log.e(TAG, "drawBrickByLevel 关卡砖块有 " + yLen + " 行，砖块数据有 " + yNum + "组.");
                continue;
            }
            int xLen = brickPosition[yCoordinate].length;
            if (xNum - 1 != xLen) {
                Log.e(TAG, "drawBrickByLevel 关卡砖块有 " + xLen + " 列，砖块数据有 " + xNum + "组.");
                continue;
            }
            for (int xCoordinate = 0; xCoordinate < xLen; xCoordinate++) {
                if (brickPosition[yCoordinate][xCoordinate] == 1) {
                    MathPoint mathPoint = new MathPoint(xCoordinate, yCoordinate);
                    mathPoint.setRange(xPositions[xCoordinate] + 3f, yPositions[yCoordinate] + 3f,
                            xPositions[xCoordinate + 1] - 3f, yPositions[yCoordinate + 1] - 3f);
                    iBrickView.addBrickView(mathPoint);
                    brickModel.addMathPoint(mathPoint);
                }
            }
        }
    }
}
