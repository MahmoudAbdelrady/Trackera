import EmptyState from "../empty-state/EmptyState";

const ServerError = () => {
  return (
    <EmptyState
      imgSrc="/Assets/error_icon.svg"
      alt="Server Error"
      title="Service Unavailable"
      message="Oops! Something went wrong on our end. Please try again later."
    />
  );
};

export default ServerError;
