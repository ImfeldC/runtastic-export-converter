package me.crespel.runtastic.parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.io.FilenameUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.topografix.gpx._1._1.GpxType;

import me.crespel.runtastic.model.ElevationData;
import me.crespel.runtastic.model.ElevationSession;
import me.crespel.runtastic.model.GpsData;
import me.crespel.runtastic.model.GpsSession;
import me.crespel.runtastic.model.GpxSession;
import me.crespel.runtastic.model.HeartRateData;
import me.crespel.runtastic.model.HeartRateSession;
import me.crespel.runtastic.model.ImagesMetaData;
import me.crespel.runtastic.model.Shoe;
import me.crespel.runtastic.model.SportSession;
import me.crespel.runtastic.model.SportSessionAlbums;
import me.crespel.runtastic.model.User;

/**
 * Sport session parser.
 * This class reads sport sessions and related data exported as JSON.
 * @author Fabien CRESPEL (fabien@crespel.net)
 * @author Christian IMFELD (imfeldc@gmail.com)
 */
public class SportSessionParser {

	public static final String ELEVATION_DATA_DIR = "Elevation-data";
	public static final String GPS_DATA_DIR = "GPS-data";
	public static final String HEARTRATE_DATA_DIR = "Heart-rate-data";
	public static final String PHOTOS_META_DATA_DIR = "Photos" + File.separator + "Images-meta-data";
	public static final String PHOTOS_SPORT_SESSION_ALBUMS_DIR = "Photos" + File.separator + "Images-meta-data" + File.separator + "Sport-session-albums";
	public static final String USER_DIR = "User";

	protected final ObjectMapper mapper = new ObjectMapper();

	public SportSession parseSportSession(File file) throws FileNotFoundException, IOException {
		return parseSportSession(file, false);
	}

	public SportSession parseSportSession(File file, boolean full) throws FileNotFoundException, IOException {
		SportSession sportSession = mapper.readValue(getFile(file), SportSession.class);
		sportSession.setFileName(file.getCanonicalPath());

		if (full) {
			File elevationDataFile = getFile(new File(new File(file.getParentFile(), ELEVATION_DATA_DIR), file.getName()));
			if (elevationDataFile.exists()) {
				sportSession.setElevationSession(parseElevationSession(elevationDataFile));
			}
			// read GPS data from JSON file
			File gpsDataFileJSON = getFile(new File(new File(file.getParentFile(), GPS_DATA_DIR), file.getName()));
			if (gpsDataFileJSON.exists()) {
				sportSession.setGpsSession(parseGpsSession(gpsDataFileJSON));
			}
			// read GPS data from GPX file (the runtastic export contains GPS data as GPX files, starting from April-2020)
			File gpsDataFileGPX = getFile(new File(new File(file.getParentFile(), GPS_DATA_DIR), FilenameUtils.getBaseName(file.getName()) + ".gpx"));
			if (gpsDataFileGPX.exists()) {
				if( sportSession.getGpxSession() == null ) {
					sportSession.setGpxSession(new GpxSession());
				}
				// Load GPX file
				try {
					JAXBContext ctx = JAXBContext.newInstance(GpxType.class);
					Unmarshaller um = ctx.createUnmarshaller();
					JAXBElement<GpxType> root = (JAXBElement<GpxType>)um.unmarshal(gpsDataFileGPX);
					GpxType gpx = root.getValue();
					sportSession.getGpxSession().setGpx(gpx);
				} catch (JAXBException e) {
					throw new RuntimeException(e);
				}
				sportSession.getGpxSession().setFileName(gpsDataFileGPX.getCanonicalPath());
			}
			File heartRateDataFile = getFile(new File(new File(file.getParentFile(), HEARTRATE_DATA_DIR), file.getName()));
			if (heartRateDataFile.exists()) {
				sportSession.setHeartRateSession(parseHeartRateSession(heartRateDataFile));
			}
		}

		// read photo session data (\Photos\Images-meta-data\Sport-session-albums)
		// From 2021 in general the file name changed from "ed613898-dd1f-4ea5-a1c8-f89bcf882dd8.json" to "2011-05-08_07-40-05-UTC_ed613898-dd1f-4ea5-a1c8-f89bcf882dd8.json"
		// But the "Sport-session-albums" are still w/o date and time.
		File photoSessionDataFile = getFile(new File(new File(file.getParentFile().getParentFile(), PHOTOS_SPORT_SESSION_ALBUMS_DIR), sportSession.getId()+".json"));
		if (photoSessionDataFile.exists()) {
			sportSession.setSessionAlbum(parseSportSessionAlbumsData(photoSessionDataFile));
			// read photo meta data (images mate data; \Photos\Images-meta-data)
			List<ImagesMetaData> images = new ArrayList<>();
			for (String photo : sportSession.getSessionAlbum().getPhotosIds()) {
				File photoMetaDataFile = getFile(new File(new File(file.getParentFile().getParentFile(), PHOTOS_META_DATA_DIR), photo + ".json"));
				if (photoMetaDataFile.exists()) {
					images.add(parseImagesMetaData(photoMetaDataFile));
				}
			}
			Collections.sort(images);
			sportSession.setImages(images);
		}
		// read and add user
		sportSession.setUser(parseUser(getFile(new File(new File(file.getParentFile().getParentFile(), USER_DIR), "user.json"))));

		if( (sportSession.getNotes() != null) && !sportSession.getNotes().equals("")) {
			List<String> tags = new ArrayList<>();
			// if a "note" is available, parse for tags leading with #
			String[] tokens = sportSession.getNotes().split(" ");
			for (String token : tokens) {
				// remove optional brackets around the tag(s)
				token = token.replace("(", "");
				token = token.replace(")", "");
				if( token.startsWith("#") ) {
					// tag found ...
					tags.add(token);
					// check if this is a "sort" tag, which can be converted into an Integer
					int foo;
					try {
						foo = Integer.parseInt(token.substring(1));
						sportSession.setSortTag(foo);
					}
					catch (NumberFormatException e)
					{
						// ignore execption, seems not to be a "sort" tag
					}
				}
			}
			sportSession.setTags(tags);
		}

		return sportSession;
	}


	public ElevationSession parseElevationSession(File file) throws FileNotFoundException, IOException {
		ElevationSession elevationsession = new ElevationSession();
		elevationsession.setFileName(file.getCanonicalPath());
		elevationsession.setElevationData(mapper.readValue(getFile(file), new TypeReference<List<ElevationData>>() {}));
		return elevationsession;
	}

	public GpsSession parseGpsSession(File file) throws FileNotFoundException, IOException {
		GpsSession gpssession = new GpsSession();
		gpssession.setFileName(file.getCanonicalPath());
		gpssession.setGpsData(mapper.readValue(getFile(file), new TypeReference<List<GpsData>>() {}));
		return gpssession;
	}

	public HeartRateSession parseHeartRateSession(File file) throws FileNotFoundException, IOException {
		HeartRateSession heartratesession = new HeartRateSession();
		heartratesession.setFileName(file.getCanonicalPath());
		heartratesession.setHeartRateData(mapper.readValue(getFile(file), new TypeReference<List<HeartRateData>>() {}));
		return heartratesession;
	}

	public SportSessionAlbums parseSportSessionAlbumsData(File file) throws FileNotFoundException, IOException {
		SportSessionAlbums album = mapper.readValue(getFile(file), new TypeReference<SportSessionAlbums>() {});
		album.setFileName(file.getCanonicalPath());
		return album;
	}


	public ImagesMetaData parseImagesMetaData(File file) throws FileNotFoundException, IOException {
		ImagesMetaData image = mapper.readValue(getFile(file), new TypeReference<ImagesMetaData>() {});
		image.setFileName(file.getCanonicalPath());
		return image;
	}

	public Shoe parseShoe(File file) throws FileNotFoundException, IOException {
		Shoe shoe = mapper.readValue(getFile(file), new TypeReference<Shoe>() {});
		shoe.setFileName(file.getCanonicalPath());
		return shoe;
	}

	public User parseUser(File file) throws FileNotFoundException, IOException {
		User user = mapper.readValue(getFile(file), new TypeReference<User>() {});
		user.setFileName(file.getCanonicalPath());
		return user;
	}

	
	private File getFile(File file)
	{
		if (file.exists()) {
			return file;
		} else {
			// From 2021 the filename in the export changed to e.g. 2012-05-25_14-56-24-UTC_2337382.json
			File[] files = new File(file.getParent()).listFiles(newfile -> newfile.getName().endsWith(file.getName()));
			if( files != null )
			{
				if (files.length > 0 )
				{
					if( files[0] != null )
					{
						return files[0];
					}
				}
			}
		}
		return file;	// return file, even if it doesn't exists
	}
}
