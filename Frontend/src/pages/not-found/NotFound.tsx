import { EmptyState } from "../../components";

const NotFound = () => {
  return (
    <EmptyState
      imgSrc="/Assets/not_found.svg"
      alt="Not Found"
      title="Not Found"
      message="Oops! The page you're looking for doesn't exist."
    />
  );
};

export default NotFound;
