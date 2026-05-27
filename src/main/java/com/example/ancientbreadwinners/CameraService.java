package com.example.ancientbreadwinners;

class CameraService {
	private final HelloApplication app;

	CameraService(HelloApplication app) {
		this.app = app;
	}

	void moveCamera(double dx, double dy) {
		app.cameraX += dx;
		app.cameraY += dy;
		clampCamera();
		app.renderer.redraw();
	}

	void clampCamera() {
		double vw = currentViewportWidth();
		double vh = currentViewportHeight();
		app.cameraX = Math.clamp(app.cameraX, 0, app.WORLD_WIDTH - vw);
		app.cameraY = Math.clamp(app.cameraY, 0, app.WORLD_HEIGHT - vh);
	}

	private double currentViewportWidth() {
		return app.worldPane.getWidth() > 0 ? app.worldPane.getWidth() : 1200;
	}

	private double currentViewportHeight() {
		return app.worldPane.getHeight() > 0 ? app.worldPane.getHeight() : 800;
	}

	void setupMinimapClick() {
		app.minimapCanvas.setOnMousePressed(e -> {
			double sx = HelloApplication.MINIMAP_W / app.WORLD_WIDTH;
			double sy = HelloApplication.MINIMAP_H / app.WORLD_HEIGHT;
			double worldX = e.getX() / sx;
			double worldY = e.getY() / sy;
			app.cameraX = worldX - currentViewportWidth() / 2;
			app.cameraY = worldY - currentViewportHeight() / 2;
			clampCamera();
			app.renderer.redraw();
			e.consume();
		});
	}
}


