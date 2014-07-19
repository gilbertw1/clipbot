(ns clipbot.plugins.hear-stuff
  (:require [clipbot.plugin :as plugin]))

(def soon-images ["http://d24w6bsrhbeh9d.cloudfront.net/photo/3265712_700b.jpg"
                  "http://d24w6bsrhbeh9d.cloudfront.net/photo/2924583_700b.jpg"
                  "http://i2.kym-cdn.com/photos/images/newsfeed/000/267/834/21b.jpg"
                  "http://i2.kym-cdn.com/photos/images/newsfeed/000/252/461/e10.jpg"
                  "http://i1.kym-cdn.com/photos/images/newsfeed/000/243/097/f65.jpg"
                  "http://i2.kym-cdn.com/photos/images/newsfeed/000/214/797/d5ebac56ef8a619e16c5c4f68d595bc5_vice_670.jpg"
                  "http://i0.kym-cdn.com/photos/images/newsfeed/000/200/105/1321312788001.jpg"
                  "http://i0.kym-cdn.com/photos/images/newsfeed/000/117/102/FmnRi.jpg"
                  "http://i1.kym-cdn.com/photos/images/newsfeed/000/117/104/1px2.jpg"])

(def fail-images ["http://www.gifsforum.com/images/meme/tattoos%20fail/grand/tattoos-fail-eccbc87e4b5ce2fe28308fd9f2a7baf3-1135.jpg"
                  "http://s2.quickmeme.com/img/34/345ee73551faa365d3f639523648de66d27fcc98e0e5c4bbe265f9c8f67dec82.jpg"
                  "http://assets.diylol.com/hfs/cc5/db9/daa/resized/joseph-ducreux-meme-generator-i-look-at-thy-effort-and-see-failure-59fe69.jpg"
                  "http://troll.me/images/yoda-senses/the-stench-of-failure-in-my-nostrils-smells-like-you-it-does.jpg"
                  "http://memeguy.com/photos/images/-failure-12943.jpg"
                  "http://treasure.diylol.com/uploads/post/image/356849/resized_chemistry-cat-meme-generator-your-test-results-are-in-100-failure-a259a0.jpg"])

(plugin/register-plugin
  {:id "hear-stuff"
   :handlers [{:regex #"soon"
               :function (fn [responder user msg]
                  (responder (rand-nth soon-images)))}
              {:regex #"FAIL"
               :function (fn [responder user msg]
                  (responder (rand-nth fail-images)))}]})