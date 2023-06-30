gcloud functions deploy is_this_thing_on \
  --gen2 \
  --region="us-central1" \
  --runtime="go120" \
  --entry-point="IsThisThingOn" \
  --trigger-topic="is-this-thing-on"