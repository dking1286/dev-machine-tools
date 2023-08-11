clj -T:build uberjar

gcloud functions deploy is_this_thing_on \
  --gen2 \
  --region="us-central1" \
  --runtime="java17" \
  --source="target/deployment" \
  --entry-point="dev.dking.dev_machine_tools.is_this_thing_on.CloudFunction" \
  --trigger-topic="is-this-thing-on"