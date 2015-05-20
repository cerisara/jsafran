package jsafran.parsing;

// Note : The command line alternative is


import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.trees.*;

import java.io.StringReader;
import java.util.Collection;
import java.util.List;

// Copied from http://nlp.stanford.edu/software/dependencies_manual.pdf
/*
The options to get the different types of representation are as follows:
-basic basic dependencies
-collapsed collapsed dependencies (not necessarily a tree structure)
-CCprocessed collapsed dependencies with propagation of conjunct
dependencies (not necessarily a tree structure)
-collapsedTree collapsed dependencies that preserve a tree structure
-nonCollapsed non-collapsed dependencies: basic dependencies as well as
the extra ones which do not preserve a tree structure
-conllx dependencies printed out in CoNLL X (CoNLL 2006) format
*/

// Mix of code from http://stackoverflow.com/questions/19429106/how-can-i-integrate-stanford-parser-software-in-my-java-program and http://stackoverflow.com/questions/20813541/parse-sentence-stanford-parser-by-passing-string-not-an-array-of-strings

public class Stanford_Dep_Parser_CollapsedTree {

	private static TokenizerFactory<CoreLabel> tokenizerFactory;
	private static LexicalizedParser parser;
	private static TreebankLanguagePack tlp = new PennTreebankLanguagePack();
	private static GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();

	static {
		System.out.println();
		tokenizerFactory = PTBTokenizer.factory(new CoreLabelTokenFactory(), "invertible=true");
		parser = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");
		parser.setOptionFlags(new String[]{"-retainTmpSubcategories"}); // option to get best performance in recognizing temporal dependencies
	}

	private String sentence;
	private Collection<TypedDependency> all_dependencies;


	public Stanford_Dep_Parser_CollapsedTree(String sentence) {
		this.sentence = sentence;
//		System.out.println("Sentence = " + sentence);
		compute_all_dependencies_of_sentence();
	}

	private void compute_all_dependencies_of_sentence() {
		List<CoreLabel> tokens = tokenize(sentence);

		Tree parsed_tree = parser.apply(tokens);

		GrammaticalStructure gs = gsf.newGrammaticalStructure(parsed_tree);
		all_dependencies = gs.typedDependenciesCollapsedTree();
	}

	public String get_collapsed_dependency_parse_with_POS_tags_string() {

		String return_text = "";

		for (TypedDependency each_dependency:all_dependencies) {
			GrammaticalRelation relation =  each_dependency.reln();
			String relation_name = relation.toString();

			IndexedWord parent_node = each_dependency.gov();
			String parent_word = parent_node.originalText();
			String parent_pos_tag = parent_node.tag();
			int parent_index = parent_node.index();
			if (parent_index == 0) { // The governor is root.
				parent_word = "ROOT"; // to avoid empty value;
				parent_pos_tag = "ROOT"; //
			}

			System.out.println("Parent = "+parent_word);

			IndexedWord child_node = each_dependency.dep();
			String child_word = child_node.originalText();
			String child_pos_tag = child_node.tag();
			int child_index = child_node.index();


			return_text = return_text + relation_name + "(" + parent_word + "-" + parent_index + "/" + parent_pos_tag + ", " + child_word + "-" + child_index + "/" + child_pos_tag + ")\n";
		}

/*		// Sample code to print an image visualizing the dependency relation. Doesn't work now because of the new package of Stanford Parser.
		try {
			writeImage(parsed_tree,all_dependencies,"dependency_parse.png",3);
		} catch (Exception e) {
			e.printStackTrace();
		}*/


		return  return_text;
	}

	private List<CoreLabel> tokenize(String str) {
		Tokenizer<CoreLabel> tokenizer =tokenizerFactory.getTokenizer(new StringReader(str));
		return tokenizer.tokenize();
	}
}