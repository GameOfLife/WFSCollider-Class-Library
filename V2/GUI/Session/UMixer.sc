UMixer {

     var <mainComposite, <mixerView, <scoreListView, font, <parent, <bounds;
     var <>scoreList;
     var <scoreController;

     *new{ |score, parent, bounds| ^super.new.init(score, parent,bounds) }

     init { |score, inParent, inBounds|

        scoreList = [score];
        font = Font( Font.defaultSansFace, 11 );
        parent = inParent ? Window("UMixer",Rect(100,1000,800,342)).front;
        bounds = inBounds ? Rect(0,0,800,342);
        this.addCurrentScoreControllers;
        mainComposite = CompositeView(parent,bounds).resize_(5)
            .background_( Color.grey(0.5) );
        this.makeMixerView

     }

     addCurrentScoreControllers {

         if( scoreController.notNil ) {
	        scoreController.remove;
	    };
        scoreController = SimpleController( scoreList.last );

		scoreController.put(\numEventsChanged, {
		    this.update;
		});
	}

	remove {
        scoreController.remove
    }

	update {
	    this.remake;
	}

    currentScore {
        ^scoreList.last
    }

    isInnerScore {
        ^(scoreList.size > 1)
    }

    remake {

        if(scoreListView.notNil){
            scoreListView.remove;
            scoreListView = nil
        };
        if(scoreList.size > 1) {
            this.makeScoreListView;
        };
        this.makeMixerView;
    }

    addtoScoreList{ |score|
        scoreList = scoreList.add(score);
        this.addCurrentScoreControllers;
        this.remake;
    }

    goToHigherScore{ |i|
        scoreList = scoreList[..i];
        this.addCurrentScoreControllers;
        fork{ { this.remake; }.defer }
    }

    makeScoreListView{
        var listSize = scoreList.size;
        scoreListView = CompositeView(mainComposite,Rect(0,0,mainComposite.bounds.width,24));
        scoreListView.addFlowLayout;
        scoreList[..(listSize-2)].do{ |score,i|
            SmoothButton(scoreListView,60@16)
                .states_([[(i+1).asString++": "++score.name, Color.black, Color.clear]])
                .font_( font )
                .border_(1).background_(Color.grey(0.8))
                .radius_(5)
                .canFocus_(false)
                .action_({
                    this.goToHigherScore(i);
                })
        };
        SmoothButton(scoreListView,16@16)
            .states_([[\up, Color.black, Color.clear]])
            .font_( font )
            .border_(1).background_(Color.grey(0.8))
            .radius_(5)
            .canFocus_(false)
            .action_({
                UMixer( this.currentScore )
            })

    }

     makeMixerView{
        var spec, maxTrack,count, color, cview,w,level,bounds, width,top,main,scroll;
		var score = this.currentScore;
		var events = score.events;
        var viewBounds = if(scoreList.size > 1) {
            mainComposite.bounds.copy.height_(mainComposite.bounds.height - 24).moveTo(0,24);
        }  {
            mainComposite.bounds.copy.moveTo(0,0)
        };

        maxTrack = events.collect{ |event| event.track }.maxItem + 1;
		count = 0;
		spec = [-90,12,\db].asSpec;

        if(mixerView.notNil) {
            mixerView.visible_(false);
            mixerView.focus(false);
            mixerView.remove;
        };

        mixerView = CompositeView(mainComposite, viewBounds);
        mixerView.addFlowLayout;

        maxTrack.do{ |j|
			events.select(_.canFreeSynth).do{ |event,i|
				var cview,faders, eventsFromFolder;
				if(event.track == j){
				color = Color.rand;
				if(event.isFolder.not){
					cview = CompositeView(mixerView,40@300);
					cview.decorator = FlowLayout(cview.bounds);
					cview.background_(Color(0.58208955223881, 0.70149253731343, 0.83582089552239, 1.0););
					cview.decorator.shift(0,24);
					EZSmoothSlider.new(cview, Rect(0,0,32,240), events.indexOf(event), spec, layout:\vert)
						.value_(event.getGain)
						.action_({ |v|
								event.setGain(v.value);
						});
					SmoothButton(cview,32@20)
					    .states_(
					        [[ \speaker, Color.black, Color.clear ],
					        [  \speaker, Color.red, Color.clear ]] )
                        .canFocus_(false)
                        .border_(1).background_(Color.grey(0.8))
                        .value_(event.muted.binaryValue)
                        .action_({ |v|
                            event.muted_(v.value.booleanValue)
                        });
				}{
					eventsFromFolder = event.allEvents.collect{ |event| (\event: event,\oldLevel: event.getGain) };
					cview = CompositeView(mixerView,40@300);
					cview.decorator = FlowLayout(cview.bounds);
					cview.background_(Color(0.28208955223881, 0.50149253731343, 0.23582089552239, 1.0););
					SmoothButton(cview,32@20).states_([["open"]])
						.radius_(3)
						.action_({
							{ this.addtoScoreList(event) }.defer(0.1)
						});
					EZSmoothSlider.new(cview, Rect(0,0,32,240), events.indexOf(event), spec, layout:\vert)
						.value_(0)
						.action_({ |v|
							eventsFromFolder.do{ |dict|
								dict[\event].setGain(dict[\oldLevel]+v.value);
							};
						});
						SmoothButton(cview,32@20)
					    .states_(
					        [[ \speaker, Color.black, Color.clear ],
					        [  \speaker, Color.red, Color.clear ]] )
                        .canFocus_(false)
                        .border_(1).background_(Color.grey(0.8))
                        .action_({ |v|
                            eventsFromFolder.do{ |dict|
								dict[\event].muted_(v.value.booleanValue)
							};
                        });
					};

				}
			}
		}


     }

     refresh{  }
}