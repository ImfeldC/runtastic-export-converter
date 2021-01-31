package me.crespel.runtastic.parser;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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
import me.crespel.runtastic.model.GpsData;
import me.crespel.runtastic.model.HeartRateData;
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
		InputStream is = getInputStream(file);
		SportSession sportSession = parseSportSession(is);
		if (full) {
			File elevationDataFile = getFile(new File(new File(file.getParentFile(), ELEVATION_DATA_DIR), file.getName()));
			if (elevationDataFile.exists()) {
				sportSession.setElevationData(parseElevationData(elevationDataFile));
			}
			// read GPS data from JSON file
			File gpsDataFileJSON = getFile(new File(new File(file.getParentFile(), GPS_DATA_DIR), file.getName()));
			if (gpsDataFileJSON.exists()) {
				sportSession.setGpsData(parseGpsData(gpsDataFileJSON));
			}
			// read GPS data from GPX file (the runtastic export contains GPS data as GPX files, starting from April-2020)
			File gpsDataFileGPX = getFile(new File(new File(file.getParentFile(), GPS_DATA_DIR), FilenameUtils.getBaseName(file.getName()) + ".gpx"));
			if (gpsDataFileGPX.exists()) {
				// Load GPX file
				try {
					JAXBContext ctx = JAXBContext.newInstance(GpxType.class);
					Unmarshaller um = ctx.createUnmarshaller();
					JAXBElement<GpxType> root = (JAXBElement<GpxType>)um.unmarshal(gpsDataFileGPX);
					GpxType gpx = root.getValue();
					sportSession.setGpx(gpx);
				} catch (JAXBException e) {
					throw new RuntimeException(e);
				}
			}
			File heartRateDataFile = getFile(new File(new File(file.getParentFile(), HEARTRATE_DATA_DIR), file.getName()));
			if (heartRateDataFile.exists()) {
				sportSession.setHeartRateData(parseHeartRateData(heartRateDataFile));
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

	public SportSession parseSportSession(InputStream is) throws FileNotFoundException, IOException {
		return mapper.readValue(is, SportSession.class);
	}


	public List<ElevationData> parseElevationData(File file) throws FileNotFoundException, IOException {
		return parseElevationData(getInputStream(file));
	}

	public List<ElevationData> parseElevationData(InputStream is) throws FileNotFoundException, IOException {
		return mapper.readValue(is, new TypeReference<List<ElevationData>>() {});
	}


	public List<GpsData> parseGpsData(File file) throws FileNotFoundException, IOException {
		return parseGpsData(getInputStream(file));
	}

	public List<GpsData> parseGpsData(InputStream is) throws FileNotFoundException, IOException {
		return mapper.readValue(is, new TypeReference<List<GpsData>>() {});
	}


	public List<HeartRateData> parseHeartRateData(File file) throws FileNotFoundException, IOException {
		return parseHeartRateData(getInputStream(file));
	}

	public List<HeartRateData> parseHeartRateData(InputStream is) throws FileNotFoundException, IOException {
		return mapper.readValue(is, new TypeReference<List<HeartRateData>>() {});
	}


	public SportSessionAlbums parseSportSessionAlbumsData(File file) throws FileNotFoundException, IOException {
		return parseSportSessionAlbumsData(getInputStream(file));
	}

	public SportSessionAlbums parseSportSessionAlbumsData(InputStream is) throws FileNotFoundException, IOException {
		return mapper.readValue(is, new TypeReference<SportSessionAlbums>() {});
	}


	public ImagesMetaData parseImagesMetaData(File file) throws FileNotFoundException, IOException {
		return parseImagesMetaData(getInputStream(file));
	}

	public ImagesMetaData parseImagesMetaData(InputStream is) throws FileNotFoundException, IOException {
		return mapper.readValue(is, new TypeReference<ImagesMetaData>() {});
	}


	public Shoe parseShoe(File file) throws FileNotFoundException, IOException {
		return parseShoe(getInputStream(file));
	}

	public Shoe parseShoe(InputStream is) throws FileNotFoundException, IOException {
		return mapper.readValue(is, new TypeReference<Shoe>() {});
	}


	public User parseUser(File file) throws FileNotFoundException, IOException {
		return parseUser(getInputStream(file));
	}

	public User parseUser(InputStream is) throws FileNotFoundException, IOException {
		return mapper.readValue(is, new TypeReference<User>() {});
	}

	private InputStream getInputStream( File userfile ) throws FileNotFoundException, IOException {
		try {
			return new BufferedInputStream(new FileInputStream(userfile));
		} catch (FileNotFoundException e) {
			// From 2021 the filename in the export changed to e.g. 2011-08-06_04-11-04-UTC_user.json
			File[] files = new File(userfile.getParent()).listFiles(file -> file.getName().endsWith(userfile.getName()));
			if( files[0] != null )
			{
					return new BufferedInputStream(new FileInputStream(files[0]));
			}
		}
		throw new FileNotFoundException("Can't find file " + userfile.getPath());
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
