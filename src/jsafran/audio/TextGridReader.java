package jsafran.audio;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jsafran.DetGraph;

/**
 * 
 * Class that contains the audio timestamps aligned with a list of DetGraph
 * These timestamps are obtained from a textgrid file which text is
 * aligned with the text in the DetGraph.
 * 
 * TODO: lorsqu'on a plusieurs tiers, il faut ne garder que ceux associes a des mots (pas des phones, commentaires...)
 * et il faut ensuite regrouper les tiers en un seul en gérant l'ordre des chevauchements correctement.
 * 
 * 
 * @author xtof
 *
 */
public class TextGridReader {
	
	/**
	 * opens a file chooser to load a textgrid
	 * 
	 * @param gs: list of graphs to align to the textgrid
	 */
	public static void load(List<DetGraph> gs) {
		
	}
	
	static private TextGridReader load(String textgridile) {
		return null;
	}
	
	private void align(List<DetGraph> gs) {
		
	}

	static class TextGridStateMachine {
		List<String> tierNames = new ArrayList<String>();
		TextGridReader infos = new TextGridReader();

		private static enum State {
			FILE_HEADER_1,
			FILE_HEADER_2,
			FILE_DESCRIPTION,
			TIER_HEADER,
			TIER_DESCRIPTION,
			INTERVAL_HEADER,
			INTERVAL_DESCRIPTION,
			DONE
		}


		private static final String
				HALFPAT_STR   = "\\s*=\\s*\"(.*)\"",
				HALFPAT_FLOAT = "\\s*=\\s*(\\d+(|\\.\\d*))",
				HALFPAT_INT   = "\\s*=\\s*(\\d+)",
				HALFPAT_INDEX = "\\s*\\[\\s*(\\d+)\\s*\\]\\s*:";


		private static final Pattern
				PAT_SIZE = Pattern.compile("size" + HALFPAT_INT),
				PAT_ITEM = Pattern.compile("item" + HALFPAT_INDEX),
				PAT_NAME = Pattern.compile("name" + HALFPAT_STR),
				PAT_TEXT = Pattern.compile("text" + HALFPAT_STR),
				PAT_XMIN = Pattern.compile("xmin" + HALFPAT_FLOAT),
				PAT_XMAX = Pattern.compile("xmax" + HALFPAT_FLOAT),
				PAT_INTERVAL_COUNT = Pattern.compile("intervals: size" + HALFPAT_INT),
				PAT_INTERVAL_INDEX = Pattern.compile("intervals" + HALFPAT_INDEX);


		class Interval {
			float xmin = -1;
			float xmax = -1;
			String text = null;

			boolean isComplete() {
				return xmin >= 0 && xmax >= 0 && text != null;
			}

			boolean findMatch(String line) {
				Matcher m;

				m = PAT_TEXT.matcher(line);
				if (m.matches()) {
					text = m.group(1).trim();
					return true;
				}

				m = PAT_XMIN.matcher(line);
				if (m.matches()) {
					xmin = Float.parseFloat(m.group(1));
					return true;
				}

				m = PAT_XMAX.matcher(line);
				if (m.matches()) {
					xmax = Float.parseFloat(m.group(1));
					return true;
				}

				return false;
			}
		}


		TextGridStateMachine(BufferedReader reader)
				throws IOException, ParsingException
		{
			State state = State.FILE_HEADER_1;
			int lineNumber = 0;
			Interval currentInterval = new Interval();
			int remainingIntervals = -1;

			while (state != State.DONE) {
				lineNumber++;
				String line = reader.readLine();
				if (line == null)
					break;
				line = line.trim();
				String lcline = line.toLowerCase();
				
				switch (state) {
					case FILE_HEADER_1:
						if (!lcline.equals("file type = \"ootextfile\""))
							throw new ParsingException(lineNumber, line, "not an ooTextFile?");
						state = State.FILE_HEADER_2;
						break;

					case FILE_HEADER_2:
						if (!lcline.equals("object class = \"textgrid\""))
							throw new ParsingException(lineNumber, line, "not a TextGrid?");
						state = State.FILE_DESCRIPTION;
						break;
					
					case FILE_DESCRIPTION:
						if (lcline.equals("item []:")) {
							state = State.TIER_HEADER;
						} else if (line.isEmpty() ||
								PAT_XMIN.matcher(lcline).matches() ||
								PAT_XMAX.matcher(lcline).matches() ||
								PAT_SIZE.matcher(lcline).matches() ||
								lcline.equals("tiers? <exists>")) {
							;
						} else {
							throw new ParsingException(lineNumber, line,
									"unknown header line");
						}
						break;
					
					case TIER_HEADER:
						if (PAT_ITEM.matcher(lcline).matches()) {
							// TODO: creer un tier d'alignement et l'ajouter aux autres tiers
//							currentTier = new Alignment();
//							tiers.add(currentTier);
							state = State.TIER_DESCRIPTION;
						} else {
							throw new ParsingException(lineNumber, line, "expecting tier header here");
						}
						break;
					
					case TIER_DESCRIPTION: {
						Matcher m;

						m = PAT_NAME.matcher(line);
						if (m.matches()) {
							tierNames.add(m.group(1));
							break;
						}

						m = PAT_INTERVAL_COUNT.matcher(line);
						if (m.matches()) {
							remainingIntervals = Integer.parseInt(m.group(1));
							state = State.INTERVAL_HEADER;
							break;
						}

						if (PAT_XMIN.matcher(lcline).matches() ||
								PAT_XMAX.matcher(lcline).matches() ||
								lcline.equals("class = \"intervaltier\"")) {
							;
						} else {
							throw new ParsingException(lineNumber, line, "unknown tier description line");
						}
						break;
					}

					case INTERVAL_HEADER:
						if (!PAT_INTERVAL_INDEX.matcher(lcline).matches()) {
							throw new ParsingException(lineNumber, line, "interval definition expected here");
						}
						state = State.INTERVAL_DESCRIPTION;
						remainingIntervals--;
						if (remainingIntervals < 0) {
							throw new ParsingException(lineNumber, line, "too many intervals");
						}
						break;

					case INTERVAL_DESCRIPTION:
						if (currentInterval.findMatch(line)) {
							if (currentInterval.isComplete()) {
								
								// TODO
//								int startFrame = TimeConverter.second2frame(currentInterval.xmin);
//								int endFrame = TimeConverter.second2frame(currentInterval.xmax);
//								if (!currentInterval.text.isEmpty() &&
//										!currentInterval.text.equals("_") &&
//										!currentInterval.text.equals("#") &&
//										!currentInterval.text.equals("%") &&
//										!currentInterval.text.equals("-") &&
//										startFrame < endFrame) { // skip silences... TODO this isn't very clean
//									currentTier.addRecognizedSegment(
//											currentInterval.text,
//											startFrame,
//											endFrame,
//											null,
//											null);
//								}
								currentInterval = new Interval();
								if (remainingIntervals == 0)
									state = State.TIER_HEADER;
								else
									state = State.INTERVAL_HEADER;
							}
						} else {
							throw new ParsingException(lineNumber, line, "unknown interval description line");
						}
						break;

					case DONE:
						break;
					default:
						throw new ParsingException("unknown state " + state);
				}
			}
		}
	}
}
