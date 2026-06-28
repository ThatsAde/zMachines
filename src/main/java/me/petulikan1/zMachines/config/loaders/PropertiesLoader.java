package me.petulikan1.zMachines.config.loaders;


import me.petulikan1.zMachines.config.Config;
import me.petulikan1.zMachines.config.Pair;
import me.petulikan1.zMachines.config.StringContainer;
import me.petulikan1.zMachines.config.constructors.DataValue;
import me.petulikan1.zMachines.config.json.Json;
import me.petulikan1.zMachines.utils.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PropertiesLoader extends EmptyLoader {

    private StringContainer lines;

    @Override
    public void load(StringContainer container, List<int[]> input) {
        reset();

        lines = container;

        /*
        *
        *
      NoStock:
        ""
        "<prefix> No items found! Add items to <shop>"
        "<prefix> by <red><key:key.sneak>+<key:key.use></red> the shop block"
        ""*/

        List<String> comments = null;

        for (int[] line : input) {
            if (lines.charAt(line[0]) == ' ') { // S-s-space?! Maybe.. this is YAML file.
                data.clear();
                comments = null;
                break;
            }

            int[] trimmed = YamlLoader.trim(lines, line);
            // Comments
            if (trimmed[0] == trimmed[1] || lines.charAt(trimmed[0]) == '#') {
                if (comments == null) {
                    comments = new ArrayList<>();
                }
                comments.add(trimmed[0] == trimmed[1] ? "" : lines.substring(trimmed[0], trimmed[1]));
                continue;
            }

            int[][] parts = readConfigLine(lines, trimmed);
            if (parts == null) { // Didn't find = symbol.. Maybe this is YAML file.
                data.clear();
                comments = null;
                break;
            }

            if (parts.length == 1) {
                if (comments != null) {
                    DataValue val = getOrCreate(lines.substring(parts[0][0], parts[0][1]));
                    val.comments = comments;
                    val.value = "";
                    comments = null;
                }
                continue;
            }

            Object readerValueParsed = YamlLoader.splitFromComment(lines, 0, parts[1]);
            int[][] readerValue = readerValueParsed instanceof Pair ? (int[][]) ((Pair) readerValueParsed).getValue() : (int[][]) readerValueParsed;
            int[] value = readerValue[0];
            int[] indexes = readerValueParsed instanceof Pair ? (int[]) ((Pair) readerValueParsed).getKey() : null;
            String comment = readerValue.length == 1 ? null : lines.substring(readerValue[1][0], readerValue[1][1]);
            set(lines.substring(parts[0][0], parts[0][1]),
                    DataValue.of(indexes == null ? lines.substring(value[0], value[1]) : YamlLoader.removeCharsAt(lines.subSequence(value[0], value[1]), indexes),
                            Json.reader().read(lines.substring(value[0], value[1])), comment, comments));
            comments = null;
        }
        if (comments != null) {
            if (comments.get(comments.size() - 1).isEmpty()) {
                comments.remove(comments.size() - 1); // just empty line
                if (comments.isEmpty()) {
                    comments = null;
                }
            }
            if (comments != null) {
                if (data.isEmpty()) {
                    header = comments;
                } else {
                    footer = comments;
                }
            }
        }
        loaded = comments != null || !data.isEmpty();
    }

    @Override
    public void load(String input) {
        if (input == null) {
            return;
        }
        StringContainer container = new StringContainer(input, 0, 0);
        load(container, LoaderReadUtil.readLinesFromContainer(container));
    }

    @Override
    public StringContainer saveAsContainer(Config config, boolean markSaved) {
        Validator.notNull(config, "Config");
        int size = config.getDataLoader().get().size();
        StringContainer builder = new StringContainer(size * 20);
        try {
            for (String h : config.getDataLoader().getHeader()) {
                builder.append(h).append(System.lineSeparator());
            }
        } catch (Exception er) {
            er.printStackTrace();
        }
        boolean first = true;
        for (Map.Entry<String, DataValue> key : config.getDataLoader().entrySet()) {
            if (first) {
                first = false;
            } else {
                builder.append(System.lineSeparator());
            }
            if (markSaved) {
                key.getValue().modified = false;
            }
            if (key.getValue().value == null) {
                builder.append(key.getKey()).append('=');
                if (key.getValue().commentAfterValue != null) {
                    builder.append(' ').append(key.getValue().commentAfterValue);
                }
                continue;
            }
            builder.append(key.getKey()).append('=').append(Json.writer().write(key.getValue().value));
            if (key.getValue().commentAfterValue != null) {
                builder.append(' ').append(key.getValue().commentAfterValue);
            }
        }
        try {
            for (String h : config.getDataLoader().getFooter()) {
                builder.append(h).append(System.lineSeparator());
            }
        } catch (Exception er) {
            er.printStackTrace();
        }
        return builder;
    }

    protected static int[][] readConfigLine(StringContainer input, int[] index) {
        boolean foundYamlIndexChar = false;
        for (int i = index[0]; i < index[1]; ++i) {
            char c = input.charAt(i);
            switch (c) {
                case ':':
                    foundYamlIndexChar = true;
                    break;
                case '=':
                    if (i == index[0]) {
                        return null; // Invalid PROPERTIES file.
                    }
                    int[][] result = new int[2][];
                    result[0] = YamlLoader.getFromQuotes(input, YamlLoader.trim(input, index[0], i));
                    result[1] = YamlLoader.trim(input, i + 1, index[1]);
                    return result;
                default:
                    break;
            }
        }
        if (foundYamlIndexChar) {
            return null; // Hey! This is YAML file.
        }
        int[][] result = new int[1][];
        result[0] = YamlLoader.getFromQuotes(input, YamlLoader.trim(input, index[0], index[1]));
        return result;
    }

    @Override
    public boolean supportsReadingLines() {
        return true;
    }

    @Override
    public String name() {
        return "properties";
    }
}
