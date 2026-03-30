package com.javacravio.tracking.util;

import com.uber.h3core.H3Core;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class H3Service {

	private final H3Core h3Core;
	private final int resolution;

	public H3Service(@Value("${cravio.h3.resolution}") int resolution) throws IOException {
		this.h3Core = H3Core.newInstance();
		this.resolution = resolution;
	}

	public String toCell(double latitude, double longitude) {
		return String.valueOf(h3Core.latLngToCell(latitude, longitude, resolution));
	}
}

