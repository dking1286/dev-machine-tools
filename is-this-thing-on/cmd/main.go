package main

import (
	"log"
	"os"

	// Blank-import the function package so the init() runs
	_ "dking.dev/is_this_thing_on"
	"github.com/GoogleCloudPlatform/functions-framework-go/funcframework"
)

func main() {
	// Use PORT environment variable, or default to 8080.
	port := "8080"
	if envPort := os.Getenv("PORT"); envPort != "" {
		port = envPort
	}

	err := funcframework.Start(port)
	if err != nil {
		log.Fatalf("funcframework.Start: %v\n", err)
	} else {
		log.Printf("Started on port %s", port)
	}
}
