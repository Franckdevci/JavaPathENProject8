package com.openclassrooms.tourguide.service;

import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.tracker.Tracker;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;

import tripPricer.Provider;
import tripPricer.TripPricer;

@Service
public class TourGuideService {
	private ExecutorService executorService = Executors.newFixedThreadPool(32);
	private Logger logger = LoggerFactory.getLogger(TourGuideService.class);
	private final GpsUtil gpsUtil;
	private final RewardsService rewardsService;
	private final TripPricer tripPricer = new TripPricer();
	public final Tracker tracker;
	boolean testMode = true;

	public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService) {
		this.gpsUtil = gpsUtil;
		this.rewardsService = rewardsService;

		Locale.setDefault(Locale.US);

		if (testMode) {
			logger.info("TestMode enabled");
			logger.debug("Initializing users");
			initializeInternalUsers();
			logger.debug("Finished initializing users");
		}
		tracker = new Tracker(this);
		addShutDownHook();
	}

	public List<UserReward> getUserRewards(User user) {
		return user.getUserRewards();
	}

	public VisitedLocation getUserLocation(User user) {
		VisitedLocation visitedLocation = (user.getVisitedLocations().size() > 0) ? user.getLastVisitedLocation()
				: trackUserLocation(user).join();
		return visitedLocation;
	}

	public User getUser(String userName) {
		return internalUserMap.get(userName);
	}

	public List<User> getAllUsers() {
		return internalUserMap.values().stream().collect(Collectors.toList());
	}

	public void addUser(User user) {
		if (!internalUserMap.containsKey(user.getUserName())) {
			internalUserMap.put(user.getUserName(), user);
		}
	}

	public List<Provider> getTripDeals(User user) {
		int cumulatativeRewardPoints = user.getUserRewards().stream().mapToInt(i -> i.getRewardPoints()).sum();
		List<Provider> providers = tripPricer.getPrice(tripPricerApiKey, user.getUserId(),
				user.getUserPreferences().getNumberOfAdults(), user.getUserPreferences().getNumberOfChildren(),
				user.getUserPreferences().getTripDuration(), cumulatativeRewardPoints);
		user.setTripDeals(providers);
		return providers;
	}

	public CompletableFuture<VisitedLocation> trackUserLocation(User user) {
		// Exécution asynchrone de la récupération de la position
		return CompletableFuture.supplyAsync(() -> {
			VisitedLocation visitedLocation = gpsUtil.getUserLocation(user.getUserId());
			user.addToVisitedLocations(visitedLocation); // Ajout des données utilisateur
			rewardsService.calculateRewards(user).join(); // Calcul des récompenses
			return visitedLocation;
		}, executorService).exceptionally(e -> {
			logger.error("Error tracking user location", e);
			return null;
		});
	}

	// la méthode renvoie les 5 plus proches attractions
	public List<Attraction> getNearByAttractions(VisitedLocation visitedLocation) {
		// Obtenir toutes les attractions
		List<Attraction> allAttractions = gpsUtil.getAttractions();

		// récuperer les 5 attractions les plus proches de l'utilisateur
		// en les triant par distance
		return allAttractions.stream()
				.sorted(Comparator.comparingDouble(
						attraction -> rewardsService.getDistance(visitedLocation.location, attraction)))
				.limit(5) // Limiter à cinq attractions
				.collect(Collectors.toList());
	}

	// la méthode renvoie une liste de maps contenant les informations des
	// attractions
	public List<Map<String, Object>> getCustomAttractionsDetails(List<Attraction> nearbyAttractions,
			VisitedLocation visitedLocation, User user) {
		List<Map<String, Object>> attractionsInfo = new ArrayList<>();

		for (Attraction attraction : nearbyAttractions) {
			// Calculer la distance de l'utilisateur à l'attraction (en miles)
			double distance = rewardsService.getDistance(visitedLocation.location, attraction);

			// Récupérer les points de récompense pour l'attraction
			int rewardPoints = rewardsService.getRewardPoints(attraction, user);

			// Construire une map pour chaque attraction
			Map<String, Object> attractionDetails = new TreeMap<>();
			attractionDetails.put("attractionName", attraction.attractionName);
			attractionDetails.put("latitude", attraction.latitude);
			attractionDetails.put("longitude", attraction.longitude);
			attractionDetails.put("distanceMiles", distance);
			attractionDetails.put("rewardPoints", rewardPoints);

			// Ajouter cette attraction à la liste
			attractionsInfo.add(attractionDetails);
		}

		return attractionsInfo;
	}

	private void addShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				tracker.stopTracking();
			}
		});
	}

	/**********************************************************************************
	 * 
	 * Methods Below: For Internal Testing
	 * 
	 **********************************************************************************/
	private static final String tripPricerApiKey = "test-server-api-key";
	// Database connection will be used for external users, but for testing purposes
	// internal users are provided and stored in memory
	private final Map<String, User> internalUserMap = new HashMap<>();

	private void initializeInternalUsers() {
		IntStream.range(0, InternalTestHelper.getInternalUserNumber()).forEach(i -> {
			String userName = "internalUser" + i;
			String phone = "000";
			String email = userName + "@tourGuide.com";
			User user = new User(UUID.randomUUID(), userName, phone, email);
			generateUserLocationHistory(user);

			internalUserMap.put(userName, user);
		});
		logger.debug("Created " + InternalTestHelper.getInternalUserNumber() + " internal test users.");
	}

	private void generateUserLocationHistory(User user) {
		IntStream.range(0, 3).forEach(i -> {
			user.addToVisitedLocations(new VisitedLocation(user.getUserId(),
					new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime()));
		});
	}

	private double generateRandomLongitude() {
		double leftLimit = -180;
		double rightLimit = 180;
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}

	private double generateRandomLatitude() {
		double leftLimit = -85.05112878;
		double rightLimit = 85.05112878;
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}

	private Date getRandomTime() {
		LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
		return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
	}

}
