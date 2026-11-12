package main

import (
	"bufio"
	"encoding/binary"
	"fmt"
	"io"
	"net"
	"os"
	"strings"
	"time"
)

// Keep in sync with MicroserviceCommandProtocol
const (
	protocolMagic   uint32 = 0x53555246
	protocolVersion uint32 = 1

	responseStandardOutput byte = 1
	responseStandardError  byte = 2
	responseCompleted      byte = 3

	maxArgumentCount = 1024
	maxStringLength  = 1024 * 1024

	commandSocketEnvironmentVariable = "SURF_MICROSERVICE_COMMAND_SOCKET"
	defaultCommandSocketPath         = "/tmp/surf-microservice.sock"

	exitUsage              = 2
	exitCommunicationError = 3
)

func main() {
	os.Exit(run(os.Args[1:]))
}

func run(command []string) int {
	if err := validateCommand(command); err != nil {
		fmt.Fprintln(os.Stderr, err)
		return exitUsage
	}

	socketPath := commandSocketPath()

	connection, err := net.DialTimeout("unix", socketPath, 3*time.Second)
	if err != nil {
		fmt.Fprintf(
			os.Stderr,
			"Could not connect to the running microservice at %s: %v\n",
			socketPath,
			err,
		)

		return exitCommunicationError
	}
	defer connection.Close()

	writer := bufio.NewWriter(connection)

	if err := writeRequest(writer, command); err != nil {
		fmt.Fprintf(
			os.Stderr,
			"Could not send command: %v\n",
			err,
		)

		return exitCommunicationError
	}

	reader := bufio.NewReader(connection)

	for {
		exitCode, completed, err := readResponse(reader)
		if err != nil {
			fmt.Fprintf(
				os.Stderr,
				"Could not read command response: %v\n",
				err,
			)

			return exitCommunicationError
		}

		if completed {
			return exitCode
		}
	}
}

func validateCommand(command []string) error {
	if len(command) == 0 {
		return fmt.Errorf("usage: microservicectl <command> [arguments...]")
	}

	if len(command) > maxArgumentCount {
		return fmt.Errorf("command exceeds the maximum argument count of %d", maxArgumentCount)
	}

	for _, argument := range command {
		if len([]byte(argument)) > maxStringLength {
			return fmt.Errorf("command argument exceeds the maximum length of %d bytes", maxStringLength)
		}
	}

	return nil
}

func commandSocketPath() string {
	configured := strings.TrimSpace(os.Getenv(commandSocketEnvironmentVariable))

	if configured != "" {
		return configured
	}

	return defaultCommandSocketPath
}

func writeRequest(writer *bufio.Writer, command []string) error {
	if err := binary.Write(writer, binary.BigEndian, protocolMagic); err != nil {
		return err
	}

	if err := binary.Write(writer, binary.BigEndian, protocolVersion); err != nil {
		return err
	}

	if err := binary.Write(writer, binary.BigEndian, uint32(len(command))); err != nil {
		return err
	}

	for _, argument := range command {
		if err := writeString(writer, argument); err != nil {
			return err
		}
	}

	return writer.Flush()
}

func readResponse(reader *bufio.Reader) (exitCode int, completed bool, err error) {
	responseType, err := reader.ReadByte()
	if err != nil {
		return 0, false, err
	}

	switch responseType {
	case responseStandardOutput:
		message, err := readString(reader)
		if err != nil {
			return 0, false, err
		}

		fmt.Fprintln(os.Stdout, message)

		return 0, false, nil

	case responseStandardError:
		message, err := readString(reader)
		if err != nil {
			return 0, false, err
		}

		fmt.Fprintln(os.Stderr, message)

		return 0, false, nil

	case responseCompleted:
		var exitCode uint32

		if err := binary.Read(reader, binary.BigEndian, &exitCode); err != nil {
			return 0, false, err
		}

		if exitCode < 0 || exitCode > 255 {
			return 0, false, fmt.Errorf(
				"invalid command exit code: %d",
				exitCode,
			)
		}

		return int(exitCode), true, nil

	default:
		return 0, false, fmt.Errorf(
			"unknown microservice command response type: %d",
			responseType,
		)
	}
}

func writeString(writer io.Writer, value string) error {
	bytes := []byte(value)

	if len(bytes) > maxStringLength {
		return fmt.Errorf(
			"protocol string exceeds the maximum length of %d bytes",
			maxStringLength,
		)
	}

	if err := binary.Write(writer, binary.BigEndian, uint32(len(bytes))); err != nil {
		return err
	}

	err := binary.Write(writer, binary.BigEndian, bytes)

	return err
}

func readString(reader io.Reader) (string, error) {
	var length uint32

	if err := binary.Read(reader, binary.BigEndian, &length); err != nil {
		return "", err
	}

	if length > maxStringLength {
		return "", fmt.Errorf(
			"invalid protocol string length: %d",
			length,
		)
	}

	bytes := make([]byte, int(length))

	if _, err := io.ReadFull(reader, bytes); err != nil {
		return "", err
	}

	return string(bytes), nil
}
