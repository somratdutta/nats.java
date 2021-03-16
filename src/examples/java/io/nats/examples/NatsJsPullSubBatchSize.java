// Copyright 2020 The NATS Authors
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at:
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package io.nats.examples;

import io.nats.client.*;

import java.time.Duration;

/**
 * This example will demonstrate basic use of a pull subscription of:
 * batch size only pull: <code>pull(int batchSize)</code>,
 * requiring manual handling of null.
 *
 * Usage: java NatsJsPullSubBatchSize [-s server] [-strm stream] [-sub subject] [-dur durable] [-mcnt msgCount]
 *   Use tls:// or opentls:// to require tls, via the Default SSLContext
 *   Set the environment variable NATS_NKEY to use challenge response authentication by setting a file containing your private key.
 *   Set the environment variable NATS_CREDS to use JWT/NKey authentication by setting a file containing your user creds.
 *   Use the URL for user/pass/token authentication.
 */
public class NatsJsPullSubBatchSize extends NatsJsPullSubBase {

    public static void main(String[] args) {
        ExampleArgs exArgs = ExampleArgs.builder()
                .defaultStream("pull-stream")
                .defaultSubject("pull-subject")
                .defaultDurable("pull-durable")
                .defaultMsgCount(99)
                //.uniqueify() // uncomment to be able to re-run without re-starting server
                .build(args);

        try (Connection nc = Nats.connect(ExampleUtils.createExampleOptions(exArgs.server))) {
            createStream(nc, exArgs.stream, exArgs.subject);

            // Create our JetStream context to receive JetStream messages.
            JetStream js = nc.jetStream();

            // start publishing the messages, don't wait for them to finish, simulating an outside producer
            publishDontWait(js, exArgs.subject, "pull-message", exArgs.msgCount);

            // Build our subscription options. Durable is REQUIRED for pull based subscriptions
            PullSubscribeOptions pullOptions = PullSubscribeOptions.builder()
                    .durable(exArgs.durable)      // required
                    // .configuration(...) // if you want a custom io.nats.client.impl.ConsumerConfiguration
                    .build();

            JetStreamSubscription sub = js.subscribe(exArgs.subject, pullOptions);
            nc.flush(Duration.ofSeconds(1));

            int red = 0;
            while (red < exArgs.msgCount) {
                sub.pull(10);
                Message m = sub.nextMessage(Duration.ofSeconds(1));
                while (m != null) {
                    if (m.isJetStream()) {
                        // process message
                        red++;
                        System.out.println("" + red + ". " + m);
                        m.ack();
                    }
                    m = sub.nextMessage(Duration.ofSeconds(1));
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
