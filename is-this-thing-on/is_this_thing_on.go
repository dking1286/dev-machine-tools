package is_this_thing_on

import (
	"context"
	"errors"
	"fmt"

	compute "cloud.google.com/go/compute/apiv1"
	"cloud.google.com/go/compute/apiv1/computepb"
	"github.com/GoogleCloudPlatform/functions-framework-go/functions"
	"github.com/cloudevents/sdk-go/v2/event"
	"google.golang.org/api/iterator"
)

func init() {
	// Register a CloudEvent function with the Functions Framework
	functions.CloudEvent("IsThisThingOn", isThisThingOn)
}

func isThisThingOn(ctx context.Context, e event.Event) error {
	fmt.Println("Hello world!")

	client, err := compute.NewInstancesRESTClient(ctx)
	if err != nil {
		fmt.Println("Couldn't get compute instance client")
		fmt.Println(err)
		return errors.New("something went wrong")
	}
	defer client.Close()

	project := "atelier-royal"
	zone := "us-central1-a"
	filter := "name=dev-machine"
	it := client.List(ctx, &computepb.ListInstancesRequest{
		Project: project,
		Zone:    zone,
		Filter:  &filter,
	})

	for {
		resp, err := it.Next()
		if err == iterator.Done {
			break
		}
		if err != nil {
			fmt.Println("Something went wrong with the response")
			fmt.Println(err)
			break
		}
		fmt.Printf("Instance %s is %s", resp.GetName(), resp.GetStatus())
	}

	// Return nil if no error occurred
	return nil
}
