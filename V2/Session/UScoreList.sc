UScoreList : UEvent {
    var <scores, <metaScore;

    *new { |...scores|
        ^super.new.init(scores)
    }

    init { |inScores|
        var currentEnd = inScores[0].duration;
        scores = inScores;

        metaScore = UScore(*scores.collect{ |score,i|
            if(i>0) {
                score.startTime_(currentEnd);
                currentEnd = currentEnd + score.duration;
            };
            score
        });
    }

    name { ^scores.collect(_.name).asString }

    waitTime { ^scores.collect(_.waitTime).sum }

    prepare { |target, startPos = 0, action|
        metaScore.prepare(target, startPos, action)
    }

    prepareAndStart{ |target, startPos = 0|
        metaScore.prepareAndStart(target, startPos)
    }

    prepareWaitAndStart { |target, startPos = 0|
        metaScore.prepareWaitAndStart(target, startPos)
    }

    start { |target, startPos, latency|
        metaScore.start(target, startPos, latency)
    }

    stop { |releaseTime, changed = true|
        metaScore.stop(releaseTime,changed)
    }

    pause { metaScore.pause }

    resume { |targets| metaScore.resume(targets) }

    release{ ^this.stop }

    gui { }

}