package com.baila;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Main {

    static String[] ignoredInPlaylist = {"z GUIDE", "Reggeatones", "BACHATA", "SHUROT"};
    static int rows = 25;
    static String[] suffixToIgnore = {"new","salsa","live"};

    public static void main(String[] args) {

        boolean deepAnalysis = true;
      /*  String dirPath = "D:\\All salsa\\alon playlist 2018\\test";//args[0];
        String resultFilePath = "D:\\All salsa\\alon playlist 2018\\test\\summery.txt";*/

        String dirPath = "D:\\All salsa\\alon playlist 2018";//args[0];
        String resultFilePath = "D:\\All salsa\\alon playlist 2018\\summery.csv";

        List<File> allPlaylists = readAllFilesFromPath(dirPath);
        List<File> wedPlaylists = allPlaylists.stream()
                .filter(file -> file.getName().contains("wed") && (!file.getName().contains("open")))
                .collect(Collectors.toList());
        List<File> satPlaylists = allPlaylists.stream()
                .filter(file -> file.getName().matches("^\\d{6}(\\d{2})?.m3u$"))
                .collect(Collectors.toList());

        Map<String, Integer> countNumberOfPlayedSongs = new HashMap<>();

        readAllPlaylists(countNumberOfPlayedSongs, wedPlaylists);
        readAllPlaylists(countNumberOfPlayedSongs, satPlaylists);

        if (deepAnalysis) {
            countNumberOfPlayedSongs = performDeepAnalysis(countNumberOfPlayedSongs);
        }
        List<String> sortedSongList = printSortResultByNumOfApperances(countNumberOfPlayedSongs);
        writeResultToFile(sortedSongList, resultFilePath);
    }

    private static Map<String, Integer> performDeepAnalysis(Map<String, Integer> countNumberOfPlayedSongs) {

        Map<String, Integer> songsNamesToAppearances = Maps.newHashMap();

        // Find mix tracks
        final List<String> mixTracks = countNumberOfPlayedSongs.keySet().stream()
                .filter(song -> song.contains("salsa mix"))
                .collect(Collectors.toList());

        // Find all other
        List<String> singleTracks = countNumberOfPlayedSongs.keySet().stream()
                .filter(song -> !mixTracks.contains(song))
                .collect(Collectors.toList());

        // Add songs to map
        List<String> mixTrackNames = mixTracks.stream().map(track -> getSongNameFromTrack(track)).collect(Collectors.toList());
        mixTrackNames.stream().forEach(track -> {
            String[] mix = track.split("_");
            Arrays.stream(mix).forEach(singleSongInMix -> {

                if ((singleSongInMix.length() > 3) && (!Arrays.asList(suffixToIgnore).contains(singleSongInMix))){
                    songsNamesToAppearances.put(singleSongInMix, 0);
                }
            });
        });

        List<String> mixAsSingleNames = Lists.newArrayList(songsNamesToAppearances.keySet());

        singleTracks.stream().forEach(track -> {
            String songName = getSongNameFromTrack(track);

            if (mixAsSingleNames.stream().filter(songInMix -> (songName.contains(songInMix))).collect(Collectors.toList()).size() == 0){
                songsNamesToAppearances.put(songName, 0);
            }
        });

        countNumberOfPlayedSongs.forEach((song, count) -> {
            List<String> possibleSongs = songsNamesToAppearances.keySet().stream()
                    .filter(shortSongName -> getSongNameFromTrack(song).contains(shortSongName)).collect(Collectors.toList());

            possibleSongs.forEach(possibleSong -> {
                int orgCount = songsNamesToAppearances.get(possibleSong);
                songsNamesToAppearances.put(possibleSong, orgCount + countNumberOfPlayedSongs.get(song));
            });

            if (possibleSongs.size() == 0){
                songsNamesToAppearances.put(song, countNumberOfPlayedSongs.get(song));
            }

        });
        return songsNamesToAppearances;


    }

    private static String getSongNameFromTrack(String track) {

        String[] path = track.split("\\\\");
        return path[path.length - 1].split(".mp3")[0].replaceAll("\\d|\\(|\\)", "").toLowerCase();
    }

    private static List<String> printSortResultByNumOfApperances(Map<String, Integer> countNumberOfPlayedSongs) {

        Map<Integer, List<String>> tempMap = new HashMap<>();
        List<String> sortedMap = new LinkedList<>();

        countNumberOfPlayedSongs.keySet().forEach(song -> {
                    if (tempMap.containsKey(countNumberOfPlayedSongs.get(song))) {
                        tempMap.get(countNumberOfPlayedSongs.get(song)).add(song);
                    } else {
                        List<String> lists = new ArrayList<>();
                        lists.add(song);
                        tempMap.put(countNumberOfPlayedSongs.get(song), lists);
                    }
                }
        );

        // return tempMap
        tempMap.keySet().stream().sorted(Comparator.reverseOrder())
                .forEach(count -> {
                    tempMap.get(count).forEach(songName -> sortedMap.add(songName + "," + count + System.getProperty("line.separator")));
                });

        return sortedMap;
    }

    private static void writeResultToFile(List<String> countNumberOfPlayedSongs, String resultFilePath) {
        File fout = new File(resultFilePath);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(fout);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

        countNumberOfPlayedSongs.stream()
                .forEach(song -> {
                    try {
                        bw.write(song);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        try {
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void readAllPlaylists(Map<String, Integer> countNumberOfPlayedSongs, List<File> playlists) {

        playlists.stream().forEach(file -> addPlayListToSongMap(file, countNumberOfPlayedSongs));
    }

    private static void addPlayListToSongMap(File file, Map<String, Integer> countNumberOfPlayedSongs) {
        List<String> allSongsInPlaylist = null;
        try {
            allSongsInPlaylist = readPlayListToLines(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // Read only lines 8 - 20
        int topRows = allSongsInPlaylist.size() > rows  ? rows : allSongsInPlaylist.size() -1;
        for (int i = 0; i < topRows; i++) {
            String songName = allSongsInPlaylist.get(i);
            if (!countNumberOfPlayedSongs.containsKey(songName)) {
                countNumberOfPlayedSongs.put(songName, 1);
            } else {
                countNumberOfPlayedSongs.put(songName, countNumberOfPlayedSongs.get(songName) + 1);
            }
        }
    }

    private static List<File> readAllFilesFromPath(String arg) {
        List<File> allFiles = null;
        try {
            allFiles = Files.walk(Paths.get(arg))
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return allFiles;
    }

    private static List<String> readPlayListToLines(File file) throws FileNotFoundException {
        List<String> list = new ArrayList<>();

        list = Arrays.asList(convertStreamToString(new FileInputStream(file)).split("\n")).stream()
                .filter(line -> !line.startsWith("#"))
                .filter(line -> !containsAny(line, ignoredInPlaylist))
                .collect(Collectors.toList());


        return list;
    }

    private static boolean containsAny(String str, String[] words) {
        boolean bResult = false;

        List<String> list = Arrays.asList(words);
        for (String word : list) {
            boolean bFound = str.contains(word);
            if (bFound) {
                bResult = bFound;
                break;
            }
        }
        return bResult;
    }

    public static String convertStreamToString(java.io.InputStream is) {
        try {
            return new java.util.Scanner(is).useDelimiter("\\A").next();
        } catch (java.util.NoSuchElementException e) {
            return "";
        }
    }
}
