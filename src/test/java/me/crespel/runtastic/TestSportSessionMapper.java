package me.crespel.runtastic;

import java.io.File;

import org.junit.Test;

import me.crespel.runtastic.mapper.DelegatingSportSessionMapper;
import me.crespel.runtastic.mapper.SportSessionMapper;
import me.crespel.runtastic.model.SportSession;
import me.crespel.runtastic.parser.SportSessionParser;

/**
 * SportSessionMapper tests.
 * @author Fabien CRESPEL (fabien@crespel.net)
 */
public class TestSportSessionMapper {

	private final SportSessionParser parser = new SportSessionParser();
	private final SportSessionMapper<?> mapper = new DelegatingSportSessionMapper();

	@Test
	public void testMapSportSessionToTCX() throws Exception {
		SportSession sportSession = parser.parseSportSession(new File("SportSession.json"));
		sportSession.setGpsSession(parser.parseGpsSession(new File("GpsData.json")));
		sportSession.setHeartRateSession(parser.parseHeartRateSession(new File("HeartRateData.json")));
		mapper.mapSportSession(sportSession, "tcx", System.out);
	}

	@Test
	public void testMapSportSessionToGPX() throws Exception {
		SportSession sportSession = parser.parseSportSession(new File("SportSession.json"));
		sportSession.setGpsSession(parser.parseGpsSession(new File("GpsData.json")));
		sportSession.setHeartRateSession(parser.parseHeartRateSession(new File("HeartRateData.json")));
		mapper.mapSportSession(sportSession, "gpx", System.out);
	}

}
