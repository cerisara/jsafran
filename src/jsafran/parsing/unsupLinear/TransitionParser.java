package jsafran.parsing.unsupLinear;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;
import de.bwaldvogel.liblinear.Train;
import jsafran.Dep;
import jsafran.DetGraph;
import jsafran.GraphIO;
import jsafran.JSafran;
import jsafran.Mot;
import jsafran.parsing.ClassificationResult;

/**
 * Transition-based parser
 * 
 * @author xtof
 *
 */
public class TransitionParser {
	enum OneAction {LA,RA,SHIFT,REDUCE};
	int[] stack, input;
	int nstack, ninput;
	// for now, we're only doing unlabeled parsing
	int bestdep=Dep.getType("_");
	Model model;
	final OneAction[] class2action = {OneAction.LA,OneAction.RA,OneAction.SHIFT,OneAction.REDUCE};
	// maximum number of features for one classification
	final int nfeatsmax = 6;
	protected int[] feats = new int[nfeatsmax];
	protected HashMap<String, Integer> feat2idx = new HashMap<String, Integer>();
	static Random random = new Random();
	
	HashMap<Integer, Double> candidate4change = new HashMap<Integer, Double>();

	/*
	 * All the methods below are used at test time
	 */
	
	interface actionDecider {
		// decides on the next action using the current state of the parser
		public OneAction getAction(DetGraph g);
	}

	public void parse(List<DetGraph> gs, actionDecider decider) {
		for (DetGraph g : gs) parse(g, decider);
	}
	public void parse(DetGraph g, actionDecider decider) {
		g.clearDeps();
		// init
		stack = new int[g.getNbMots()];
		input = new int[stack.length];
		nstack=0;
		ninput=input.length;
		for (int i=0,j=input.length-1;i<input.length;i++,j--) input[j]=i;

		while (ninput>0) {
			OneAction a = decider.getAction(g);
//			System.out.println("bestaction "+a);
			switch (a) {
			case LA:
				if (nstack>0) {
					// this should never occur, but who knows...
					int gov=stack[nstack-1], head=input[ninput-1];
					g.ajoutDep(bestdep, gov, head);
					--nstack;
				} else {
//					System.out.println("WARNING: LA with empty stack");
					// do a shift to prevent infinite loops
					stack[nstack++]=input[--ninput];
				}
				break;
			case RA:
				if (nstack>0) {
					int head=stack[nstack-1]; int gov=input[ninput-1];
					g.ajoutDep(bestdep, gov, head);
					stack[nstack++]=input[--ninput];
				} else {
//					System.out.println("WARNING: RA with empty stack");
					// do a shift to prevent infinite loops
					stack[nstack++]=input[--ninput];
				}
				break;
			case REDUCE:
				if (nstack>0) {
					--nstack;
				} else {
//					System.out.println("WARNING: REDUCE with empty stack");
					// do a shift to prevent infinite loops
					stack[nstack++]=input[--ninput];
				}
				break;
			case SHIFT:
				stack[nstack++]=input[--ninput];
				break;
			}
//			System.out.println("parse stacks "+a);
//			System.out.println("\t"+nstack+" "+Arrays.toString(stack));
//			System.out.println("\t"+ninput+" "+Arrays.toString(input));
		}
	}

	private int getIdx(String f) {
		Integer i = feat2idx.get(f);
		if (i==null) {
			i=feat2idx.size();
			feat2idx.put(f, i);
		}
		return i;
	}

	/*
	 * here are all the features used by the maltparser:
	 * 
F1 OK   <feature>InputColumn(POSTAG, Stack[0])</feature>
F2 OK   <feature>InputColumn(POSTAG, Input[0])</feature>
F6 OK   <feature>InputColumn(POSTAG, Input[1])</feature>
        <feature>InputColumn(POSTAG, Input[2])</feature>
        <feature>InputColumn(POSTAG, Input[3])</feature>
F5 OK   <feature>InputColumn(POSTAG, Stack[1])</feature>
        <feature>Merge(InputColumn(POSTAG, Stack[0]), InputColumn(POSTAG, Input[0]))</feature>
        <feature>Merge3(InputColumn(POSTAG, Stack[1]), InputColumn(POSTAG, Stack[0]), InputColumn(POSTAG, Input[0]))</feature>
        <feature>Merge3(InputColumn(POSTAG, Stack[0]), InputColumn(POSTAG, Input[0]), InputColumn(POSTAG, Input[1]))</feature>
        <feature>Merge3(InputColumn(POSTAG, Input[0]), InputColumn(POSTAG, Input[1]), InputColumn(POSTAG, Input[2]))</feature>
        <feature>Merge3(InputColumn(POSTAG, Input[1]), InputColumn(POSTAG, Input[2]), InputColumn(POSTAG, Input[3]))</feature>
        <feature>OutputColumn(DEPREL, Stack[0])</feature>
        <feature>OutputColumn(DEPREL, ldep(Stack[0]))</feature>
        <feature>OutputColumn(DEPREL, rdep(Stack[0]))</feature>
        <feature>OutputColumn(DEPREL, ldep(Input[0]))</feature>
        <feature>Merge3(InputColumn(POSTAG, Stack[0]), OutputColumn(DEPREL, ldep(Stack[0])), OutputColumn(DEPREL, rdep(Stack[0])))</feature>
        <feature>Merge(InputColumn(POSTAG, Stack[0]), OutputColumn(DEPREL, Stack[0]))</feature>
        <feature>Merge(InputColumn(POSTAG, Input[0]), OutputColumn(DEPREL, ldep(Input[0])))</feature>
F3 OK   <feature>InputColumn(FORM, Stack[0])</feature>
F4 OK   <feature>InputColumn(FORM, Input[0])</feature>
        <feature>InputColumn(FORM, Input[1])</feature>
        <feature>InputColumn(FORM, head(Stack[0]))</feature>
	 */
	
	/*
	 * I obtain with Metropolis training and F1+F2+F3+F4 on the training corpus: UAS=53.1%
	 * 
	 */
	public int getFeats(DetGraph g) {
		int nfeats=0;
        if (nstack>0) {
            String x=g.getMot(stack[nstack-1]).getForme();
            feats[nfeats++]=getIdx("STAW"+x);
            x=g.getMot(stack[nstack-1]).getPOS();
            feats[nfeats++]=getIdx("STAP"+x);
            if (nstack>1) {
                x=g.getMot(stack[nstack-2]).getPOS();
                feats[nfeats++]=getIdx("STA1P"+x);
            }
        }
		if (ninput>0) {
			String x=g.getMot(input[ninput-1]).getForme();
			feats[nfeats++]=getIdx("INPW"+x);
			x=g.getMot(input[ninput-1]).getPOS();
			feats[nfeats++]=getIdx("INPP"+x);
            if (ninput>1) {
                x=g.getMot(input[ninput-2]).getPOS();
                feats[nfeats++]=getIdx("INP1P"+x);
            }
		}
		return nfeats;
	}
	/**
	 * attention; on n'a pas toujours le meme nb de features !
	 * @return
	 */
	public int[] getFeats() {return feats;}

	/**
	 * This method uses LIBLINEAR, and not my own SimplifiedLinearModel
	 * 
	 * @param g
	 * @return
	 */
	private OneAction getBestAction(DetGraph g) {
		List<Feature> x = new ArrayList<Feature>();
		int nfeats = getFeats(g);
		for (int i=0;i<nfeats;i++) {
			Feature node = new de.bwaldvogel.liblinear.FeatureNode(feats[i]+1, 1);
			x.add(node);
		}
		de.bwaldvogel.liblinear.Feature[] nodes = new Feature[x.size()];
		nodes = x.toArray(nodes);
		int y=(int)Linear4sampling.predict(model, nodes);
		candidate4change.putAll(Linear4sampling.dims2change);
		--y;
		return class2action[y];
	}

	/*
	 * All the methods below are used at training time
	 */
	
	interface Scorer {
		public double score(List<DetGraph> gs);
	}
	class SupervisedScorer implements Scorer {
		List<DetGraph> ref = new ArrayList<DetGraph>();
		public SupervisedScorer(List<DetGraph> gold) {
		    for (int i=0;i<gold.size();i++)
		        ref.add(gold.get(i).clone());
		}
		@Override
		public double score(List<DetGraph> gs) {
			assert gs.size()==ref.size();
			List<DetGraph> gsrec=gs, gsref=ref;
//			if (gs.size()>500) {
//				// it's useless to score on more than 500 sentences, it would take too much time
//				gsrec=gs.subList(0, 500);
//				gsref=ref.subList(0, 500);
//			}
			float[] las = ClassificationResult.calcErrors(gsrec, gsref);
			float uas = las[1];
			return uas;
		}
	}

	public double testAndCalcUAS(final List<DetGraph> gold) {
	    ArrayList<DetGraph> gs = new ArrayList<DetGraph>();
	    for (int i=0;i<gold.size();i++) {
	        DetGraph g = gold.get(i).clone();
	        g.cursent=i;
	        gs.add(g);
	    }
	    parse(gs, new actionDecider() {
            @Override
            public OneAction getAction(DetGraph g) {
                // decides based on the linear model
                return getBestAction(g);
            }
        });
	    Scorer UASScorer = new Scorer() {
            @Override
            public double score(List<DetGraph> gs) {
                float[] las = ClassificationResult.calcErrors(gs, gold);
                float uas = las[1];
                return uas;
            }
        };
        double uas = UASScorer.score(gs);
        System.out.println("UAS "+uas);
        return uas;
	}
	
	public void trainGibbs(List<DetGraph> gs, Scorer scorer) {
		final int nbins=50;
		
		// pre-build all possible features
		feat2idx.clear();
		samplePaths(gs);

		bestsc=0;
		{
			// init
			createRandomModel();
			parse(gs, new actionDecider() {
				@Override
				public OneAction getAction(DetGraph g) {
					// decides based on the linear model
					return getBestAction(g);
				}
			});
			double sc = scorer.score(gs);
//			DetGraph[] gg = {gs.get(0),((SupervisedScorer)scorer).ref.get(0)};
//			JSafran.viewGraph(gg);
			System.out.println("init model score "+sc);
		}
		
		final int niters = 1000;
		double[] post = new double[nbins];
		double sc=0;
		for (int iter=0;iter<niters;iter++) {
			for (int dim=0;dim<model.w.length;dim++) {
				// in this dimension, look for the best value
				// sample this dimension in bins
				final double vmin=0, vmax=10, delta=(vmax-vmin)/(double)nbins;
				int bin=0;
				for (double v=vmin; v<vmax && bin<nbins;v+=delta,bin++) {
					model.w[dim]=v;
					parse(gs, new actionDecider() {
						@Override
						public OneAction getAction(DetGraph g) {
							// decides based on the linear model
							return getBestAction(g);
						}
					});
					// for each possible value, we compute the score obtained with this solution
					post[bin] = scorer.score(gs);
				}
				// chooses randomly according to post
				int d = sample_Mult(post);
				model.w[dim]=d;
				sc=post[d];
				if (sc>bestsc) bestsc=sc;
			}
			System.out.println("training iter "+iter+" score "+sc+" "+bestsc);
		}
	}

	/**
	 * speed-up sampling by:
	 * - sampling dim according to the frequency of this feature in the corpus
	 * - sampling delta so that it impacts on at least one arc in the corpus
	 * 
	 * @param gs
	 * @param scorer
	 */
	public void trainMetropolis(List<DetGraph> gs, Scorer scorer) {
	    final boolean keepPrevModel = false;
	    
	    if (keepPrevModel) {
            // use a pre-trained model as init
	    } else {
	        // pre-build all possible features
	        // TODO: compute the freq of features
	        feat2idx.clear();
	        samplePaths(gs);
            // random init
            createRandomModel();
	    }
        
	    {
            parse(gs, new actionDecider() {
                @Override
                public OneAction getAction(DetGraph g) {
                    // decides based on the linear model
                    return getBestAction(g);
                }
            });
	        double sc = scorer.score(gs);
	        //			DetGraph[] gg = {gs.get(0),((SupervisedScorer)scorer).ref.get(0)};
	        //			JSafran.viewGraph(gg);
	        System.out.println("init model score "+sc);
	        bestsc=sc;
	    }
		
		final int niters = 100000000;
		double prevsc=0;
		long nacc=0, ntot=0;
		for (int iter=0;iter<niters;iter++) {
			int dim = random.nextInt(model.w.length);
			float delta = random.nextFloat()-0.5f;
			delta/=5f; // this gives a random float between [-0.1;0.1]
			delta*=model.w[dim]; // this modifies the weights by +/-10% maximum
			int ncands = candidate4change.size();
			
			if (false) {
				// smarter sampling ?
				int i=random.nextInt(candidate4change.size());
				ArrayList<Integer> ll = new ArrayList<Integer>(candidate4change.keySet());
				dim = ll.get(i);
				delta = candidate4change.get(dim).floatValue();
			}
			
			setNewWeight(dim,model.w[dim]+delta);
			candidate4change.clear();
			parse(gs, new actionDecider() {
				@Override
				public OneAction getAction(DetGraph g) {
					// decides based on the linear model
					return getBestAction(g);
				}
			});
			double sc = scorer.score(gs), newsc=sc;
			
//			System.out.println("debug "+sc+" "+dim+" "+delta+" "+model.w[dim]);
//			JSafran.viewGraph(gs);
			
			nacc++; ntot++;
			if (sc<prevsc) {
				double ar = sc/prevsc;
				ar/=10f;
				if (random.nextDouble()>ar) {
		            setNewWeight(dim,model.w[dim]-delta);
					newsc=prevsc;
					nacc--; nacc--;
				}
			} else if (sc>bestsc) {
				bestsc=sc;
			}
			// on a un acceptance rate de 99% car tous les sc=prevsc=0 !
			if (iter%1000==0) {
				float ar = (float)nacc/(float)ntot;
				System.out.println("training iter "+iter+" score "+prevsc+" -> "+sc+" "+ar+" "+bestsc+" ncands "+ncands+" "+delta);
			}
			prevsc=newsc;
		}
	}
	
	public void trainClassic(final List<DetGraph> gold) {
	    /*
	     * - If stack is not root AND gold head of stack == input, then LEFTARC
	     * - else if gold head of input = stack, then RIGHTARC
	     * - else if stack has no gold head, then SHIFT
	     * - else if input has left dependent AND left-most dependent < stack, then REDUCE
	     * - else if gold head of input < stack, then REDUCE
	     * - else SHIFT
	     * 
	     * 
	    if (!stackPeek.isRoot() && gold.getTokenNode(stackPeekIndex).getHead().getIndex() == inputPeekIndex) {
            return updateActionContainers(ArcEager.LEFTARC, gold.getTokenNode(stackPeekIndex).getHeadEdge().getLabelSet());
        } else if (gold.getTokenNode(inputPeekIndex).getHead().getIndex() == stackPeekIndex) {
            return updateActionContainers(ArcEager.RIGHTARC, gold.getTokenNode(inputPeekIndex).getHeadEdge().getLabelSet());
        } else if (!nivreConfig.isAllowReduce() && !stackPeek.hasHead()) {
            return updateActionContainers(ArcEager.SHIFT, null);
        } else if (gold.getTokenNode(inputPeekIndex).hasLeftDependent() &&
                gold.getTokenNode(inputPeekIndex).getLeftmostDependent().getIndex() < stackPeekIndex) {
            return updateActionContainers(ArcEager.REDUCE, null);
        } else if (gold.getTokenNode(inputPeekIndex).getHead().getIndex() < stackPeekIndex && 
                (!gold.getTokenNode(inputPeekIndex).getHead().isRoot() || nivreConfig.isAllowRoot())) {
            return updateActionContainers(ArcEager.REDUCE, null);
        } else {
            return updateActionContainers(ArcEager.SHIFT, null);
        }
        */
	    
	    final ArrayList<Double> labs = new ArrayList<Double>();
	    final ArrayList<Feature[]> fts = new ArrayList<Feature[]>();
	    
	    ArrayList<DetGraph> gs = new ArrayList<DetGraph>();
	    for (int i=0;i<gold.size();i++) {
	        DetGraph g = gold.get(i).clone();
	        g.cursent=i;
	        gs.add(g);
	    }
	    for (DetGraph g : gs) {
	        g.clearDeps();
	        parse(g, new actionDecider() {
                @Override
                public OneAction getAction(DetGraph grec) {
                    DetGraph g = gold.get(grec.cursent);
                    OneAction res=OneAction.SHIFT;
                    if (nstack>0) {
                        int st = stack[nstack-1];
                        int in = input[ninput-1];
                        int d = g.getDep(st);
                        if (d>=0) {
                            // stack has a head
                            int sthd = g.getHead(d);
                            if (sthd==in) res=OneAction.LA;
                        }
                        if (res==OneAction.SHIFT) {
                            d = g.getDep(in);
                            if (d>=0) {
                                // input has a head
                                int inhd = g.getHead(d);
                                if (inhd==st) {
                                    res=OneAction.RA;
                                }
                            }
                            if (res==OneAction.SHIFT) {
                                d = g.getDep(st);
                                if (d>=0) {
                                    // stack has a head
                                    int leftdep = g.getLeftmostSubtreeNode(in);
                                    if (leftdep>=0&&leftdep<st) {
                                        res=OneAction.REDUCE;
                                    } else {
                                        d = g.getDep(in);
                                        if (d>=0) {
                                            // input has a head
                                            int inhd = g.getHead(d);
                                            if (inhd<st) res=OneAction.REDUCE;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    int nfeats = getFeats(grec);
                    Feature[] ff = new Feature[nfeats];
                    Arrays.sort(feats,0,nfeats);
                    for (int i=0;i<nfeats;i++) {
                        ff[i] = new de.bwaldvogel.liblinear.FeatureNode(feats[i]+1, 1);
                    }
                    fts.add(ff);
                    double y = res.ordinal()+1;
                    labs.add(y);
                    return res;
                }
            });
	    }
	    
	    System.out.println("classic training with nfeats "+feat2idx.size()+" "+fts.size()+" "+labs.size());
	    Problem pb = Train.constructProblem(labs, fts, feat2idx.size(), 0);
	    Parameter parms = new Parameter(SolverType.valueOf("L2R_LR"), 1, 0.01);
	    model = Linear.train(pb, parms);
	    try {
	        model.save(new File("liblinear.mod"));
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}

	private void setNewWeight(int dim, double neww) {
	    model.w[dim]=neww;
	    double sum=0;
	    for (int i=0;i<model.w.length;i++) sum+=model.w[i];
	    if (Math.abs(sum)<0.0001) {
	        System.out.println("WARNING: weights sum nearly to 0 "+sum);
	        double c = 1./(double)model.w.length;
	        for (int i=0;i<model.w.length;i++) model.w[i]+=c;
	    } else
	        for (int i=0;i<model.w.length;i++) model.w[i]/=sum;
	}
	
	int sample_Mult(double[] th) {
		double s=0;
		for (int i=0;i<th.length;i++) s+=th[i];
		s *= random.nextDouble();
		for (int i=0;i<th.length;i++) {
			s-=th[i];
			if (s<0) return i;
		}
		return 0;
	}

	/**
	 * The scorer may be the LAS in case of supervised training, or the risk estimate in case of unsupervised training
	 * WARNING: gs is overriten during training
	 * The gold must not be given here, but in the scorer !
	 */
	public void trainCoordinateClimb(List<DetGraph> gs, Scorer scorer) {
		final int nbins=50;
		
		// pre-build all possible features
		feat2idx.clear();
		samplePaths(gs);

		{
			// init
			createRandomModel();
			parse(gs, new actionDecider() {
				@Override
				public OneAction getAction(DetGraph g) {
					// decides based on the linear model
					return getBestAction(g);
				}
			});
			double sc = scorer.score(gs);
//			DetGraph[] gg = {gs.get(0),((SupervisedScorer)scorer).ref.get(0)};
//			JSafran.viewGraph(gg);
			System.out.println("init model score "+sc);
		}
		
		// coordinate-wise training
		final int niters = 1000;
		double bestsc=0;
		for (int iter=0;iter<niters;iter++) {
			for (int dim=0;dim<model.w.length;dim++) {
				// in this dimension, look for the best value
				// sample this dimension in bins
				final double vmin=0, vmax=10, delta=(vmax-vmin)/(double)nbins;
				double scmax = Double.MIN_VALUE;
				double bestv = 0;
				for (double v=vmin; v<vmax;v+=delta) {
					model.w[dim]=v;
					parse(gs, new actionDecider() {
						@Override
						public OneAction getAction(DetGraph g) {
							// decides based on the linear model
							return getBestAction(g);
						}
					});
					// for each possible value, we compute the score obtained with this solution
					double sc = scorer.score(gs);
					if (sc>scmax) {
						bestsc=sc;
						scmax=sc;
						bestv=v;
					}
				}
				model.w[dim]=bestv;
			}
			System.out.println("training iter "+iter+" score "+bestsc);
		}
	}
	
	private double bestsc=0;
	private void trainGridRecurs(List<DetGraph> gs, Scorer scorer, int dim) {
		final int nbins=10;
		if (dim>=model.w.length) {
			parse(gs, new actionDecider() {
				@Override
				public OneAction getAction(DetGraph g) {
					// decides based on the linear model
					return getBestAction(g);
				}
			});
			double sc = scorer.score(gs);
			System.out.println(Arrays.toString(model.w)+" "+sc);
			if (sc>bestsc) {
				bestsc=sc;
				System.out.println("bestsc "+bestsc);
			}
			return;
		}
		final double vmin=0, vmax=10, delta=(vmax-vmin)/(double)nbins;
		for (int i=0;i<nbins;i++) {
			for (double v=vmin; v<vmax;v+=delta) {
				model.w[dim]=v;
				trainGridRecurs(gs, scorer, dim+1);
			}
		}
	}
	/**
	 * This method is impossible to use except for toy examples
	 * @param gs
	 * @param scorer
	 */
	public void trainGrid(List<DetGraph> gs, Scorer scorer) {
		// pre-build all possible features
		feat2idx.clear();
		samplePaths(gs);
		createRandomModel(); // just to allocate the weights
		bestsc=0;
		trainGridRecurs(gs, scorer, 0);
	}
	
	// ====================================================
	/** 
	 this method is used during training, in case we encounter a new feature that has never been seen before.
	 But this methods costs a lot, so before training, we sample as many paths as possible to compute the largest possible set of features
	 */
	void increaseModelWeights() {
		double[] newWeights = Arrays.copyOf(model.w, model.w.length+1);
		newWeights[newWeights.length-1] = Math.round(random.nextDouble() * 100000.0) / 10000.0;
		model.w = newWeights;
		model.nr_feature = model.w.length / model.label.length - 1;
	}
	/** 
	 this method is used before training: it randomly samples paths so as to compute (all ?) possible features
	 in advance, to build the weights array
	 */
	void samplePaths(List<DetGraph> gs) {
		int nsamples = 100;
		for (int i=0;i<nsamples;i++) {
			for (int j=0;j<gs.size();j++) {
				DetGraph g = gs.get(j);
				parse(g, new actionDecider() {
					@Override
					public OneAction getAction(DetGraph g) {
						// decides randomly, but before compute the features, because the goal is to list all possible features
						getFeats(g);
						int a = random.nextInt(OneAction.values().length);
						return OneAction.values()[a];
					}
				});
			}
		}
//		System.out.println("sample paths, nb of features "+feat2idx.size());
//		System.out.println("feats: "+feat2idx.keySet());
	}
	/** 
	 * this method is used at initialization for training
	 */
	void createRandomModel() {
		model = new Model();
		model.solverType = SolverType.L2R_LR;
		model.bias = 2;
		model.label = new int[] {1,2,3,4};
		model.w = new double[model.label.length * feat2idx.size()];
		double sum=0;
		for (int i = 1; i < model.w.length; i++) {
			// precision should be at least 1e-4
			model.w[i] = Math.round(random.nextDouble() * 100000.0) / 10000.0;
			sum+=model.w[i];
		}
		// force the sum of weights = 1
// TODO: BUGFIX: the sum should be one PER FEATURE
		model.w[0]=1.-sum;

		model.nr_feature = model.w.length / model.label.length - 1;
		model.nr_class = model.label.length;
	}

	void test1() {
		DetGraph g=new DetGraph();
		int i=0;
		g.addMot(i++, new Mot("il", "je", "PRO"));
		g.addMot(i++, new Mot("dort", "dormir", "VER"));
		g.addMot(i++, new Mot("dans", "dans", "PREP"));
		g.addMot(i++, new Mot("la", "le", "DET"));
		g.addMot(i++, new Mot("cour", "cour", "NOM"));

		ArrayList<DetGraph> gs = new ArrayList<DetGraph>();
		gs.add(g);
		samplePaths(gs);
		createRandomModel();
		parse(g, new actionDecider() {
			@Override
			public OneAction getAction(DetGraph g) {
				// decides based on the linear model
				return getBestAction(g);
			}
		});
		System.out.println("parsing done");
		JSafran.viewGraph(g);
	}
	void test2() {
		DetGraph g=new DetGraph();
		int i=0;
		g.addMot(i++, new Mot("il", "je", "PRO"));
		g.addMot(i++, new Mot("dort", "dormir", "VER"));
		g.addMot(i++, new Mot("dans", "dans", "PREP"));
		g.addMot(i++, new Mot("la", "le", "DET"));
		g.addMot(i++, new Mot("cour", "cour", "NOM"));
		g.ajoutDep("_", 0, 1);
		g.ajoutDep("_", 3, 4);
		g.ajoutDep("_", 4, 2);
		g.ajoutDep("_", 2, 1);

		ArrayList<DetGraph> gs = new ArrayList<DetGraph>();
		gs.add(g);
		SupervisedScorer supervisedScorer = new SupervisedScorer(gs);
//		trainCoordinateClimb(gs, supervisedScorer);
//		trainGibbs(gs, supervisedScorer);
		trainMetropolis(gs, supervisedScorer);
	}
    void trainMetropolis() {
        GraphIO gio = new GraphIO(null);
        List<DetGraph> gs = gio.loadAllGraphs("train2011.xml");
//        gs=gs.subList(0, 10);
        SupervisedScorer supervisedScorer = new SupervisedScorer(gs);
        trainClassic(gs);
        testAndCalcUAS(supervisedScorer.ref);
        trainMetropolis(gs, supervisedScorer);
    }
    /**
     * This method is used to check whether our features are good enough and also to check whether Metropolis has converged, by
     * giving the target performances that should be obtained after classical gradient training.
     * 
     * With only F1+F2+F3+F4, this gives  UAS=70.34% on the full training corpus
     * With F1+F2+F3+F4+F5+F6, this gives UAS=71.91%
     */
    void trainAndTestClassicSup() {
        GraphIO gio = new GraphIO(null);
        List<DetGraph> gs = gio.loadAllGraphs("train2011.xml");
        gs=gs.subList(0, 10);
        trainClassic(gs);
        testAndCalcUAS(gs);
    }
    void trainMyGradient() {
        final ActionCorpus corp = new ActionCorpus();
        corp.buildCorpus(this);
        System.out.println("corpus ready");
        
        // (the features have already been created in buildcorpus)
        final SimplifiedLinearModel mod = new SimplifiedLinearModel(feat2idx.size(), OneAction.values().length);
        mod.randomInit();
        
        // train
        GradientTrainer tr = new GradientTrainer();
        tr.train(mod.w, new GradientTrainer.Scorer() {
            @Override
            public float getScore(float[] parms) {
                return mod.getNegError(corp);
            }
        });
    }
	public static void main(String args[]) {
		TransitionParser m = new TransitionParser();
//		m.trainAndTestClassicSup();
		m.trainMetropolis();
//		m.trainMyGradient();
	}
}
